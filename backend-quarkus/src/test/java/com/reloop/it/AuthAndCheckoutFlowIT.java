package com.reloop.it;

import com.reloop.support.DockerRequired;
import com.reloop.support.PostgresTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Full-stack smoke test: security, public reads, auth-guarded endpoints, and
 * the anti-hoarding checkout lease against a real PostgreSQL (Flyway-seeded).
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@DockerRequired
class AuthAndCheckoutFlowIT {

    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction userTransaction;

    @Test
    @DisplayName("Register -> login -> auth-guarded access -> lease -> confirm payment (escrow held)")
    void registerLoginLeaseCheckout() throws Exception {
        String email = "it-buyer-" + UUID.randomUUID() + "@reloop.test";

        // 1. Register
        given().contentType("application/json")
                .body(Map.of("email", email, "password", "S3curePass!x", "fullName", "IT Buyer"))
                .when().post("/api/v1/auth/register")
                .then().statusCode(201)
                .body("success", equalTo(true))
                .body("data.accessToken", notNullValue())
                .body("data.refreshToken", notNullValue());

        // 2. Login
        Map<String, String> login = given().contentType("application/json")
                .body(Map.of("email", email, "password", "S3curePass!x"))
                .when().post("/api/v1/auth/login")
                .then().statusCode(200)
                .body("data.role", equalTo("CUSTOMER"))
                .extract().body().jsonPath().getMap("data", String.class, String.class);
        String accessToken = login.get("accessToken");

        // 3. Correlation id must round-trip
        String corrId = UUID.randomUUID().toString();
        given().header("X-Correlation-ID", corrId)
                .when().get("/api/v1/catalog/models")
                .then().statusCode(200)
                .header("X-Correlation-ID", corrId);

        // Invalid correlation id is rejected like the legacy filter did
        given().header("X-Correlation-ID", "not-a-uuid")
                .when().get("/api/v1/catalog/models")
                .then().statusCode(400);

        // 4. Protected endpoint without token is 401
        given().when().get("/api/v1/warranties/my")
                .then().statusCode(401);

        // 5. Seed one LISTED unit + ACTIVE listing owned by a test seller
        UUID unitId = seedListedUnitAndListing();
        UUID listingId = listingIdFor(unitId);

        // 6. Acquire 15-minute lease
        var reserveBody = Map.of("unitId", unitId.toString(), "listingId", listingId.toString());
        var reservation = given().contentType("application/json")
                .header("Authorization", "Bearer " + accessToken)
                .body(reserveBody)
                .when().post("/api/v1/checkout/reserve")
                .then().statusCode(201)
                .extract().body().jsonPath();
        assertThat(reservation.getFloat("data.remainingSeconds")).isGreaterThan(0);
        UUID reservationToken = UUID.fromString(reservation.getString("data.token"));

        // 7. Confirm payment -> escrow held, 85/15 split
        String idempotencyKey = "IT-" + UUID.randomUUID();
        String confirmBody = "{\"reservationToken\":\"" + reservationToken + "\"," +
                "\"paymentMethod\":\"SIMULATED_ESCROW_DIRECT\"," +
                "\"shippingAddress\":\"Jl. IT Test No 1, Jakarta\"}";
        var confirmation = given().contentType("application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Idempotency-Key", idempotencyKey)
                .body(confirmBody)
                .when().post("/api/v1/checkout/confirm-payment")
                .then().statusCode(200)
                .extract().body().jsonPath();

        assertThat(confirmation.getString("data.paymentStatus")).isEqualTo("PAID");
        assertThat(confirmation.getString("data.escrowStatus")).isEqualTo("HELD");
        assertThat(confirmation.getFloat("data.totalAmount")).isEqualTo(10000000.00f);
        assertThat(confirmation.getFloat("data.platformFeeAmount")).isEqualTo(1500000.00f);
        assertThat(confirmation.getFloat("data.sellerNetAmount")).isEqualTo(8500000.00f);
        String orderNumber = confirmation.getString("data.orderNumber");

        // 8. Idempotent replay with the SAME key returns the cached confirmation
        given().contentType("application/json")
                .header("Authorization", "Bearer " + accessToken)
                .header("Idempotency-Key", idempotencyKey)
                .body(confirmBody)
                .when().post("/api/v1/checkout/confirm-payment")
                .then().statusCode(200)
                .body("data.orderNumber", equalTo(orderNumber));
    }

    @Test
    @DisplayName("Trade-in valuation endpoint returns server-authoritative offer")
    void tradeInCalculation() {
        var offer = given().contentType("application/json")
                .body(Map.of(
                        "msrp", 10000000.00,
                        "annualDepreciationRate", 0.150,
                        "releaseDate", LocalDate.now().minusYears(1).toString(),
                        "condition", "EXCELLENT",
                        "functionality", "FULLY_FUNCTIONAL",
                        "batteryHealthPercentage", 95,
                        "hasCompleteAccessories", true))
                .when().post("/api/v1/trade-in/calculate")
                .then().statusCode(200)
                .body("success", equalTo(true))
                .extract().body().jsonPath();

        assertThat(offer.getFloat("data.conditionMultiplier")).isEqualTo(0.95f);
        assertThat(offer.getFloat("data.estimatedOffer")).isBetween(7000000.0f, 7200000.0f);
    }

    private UUID listingIdFor(UUID unitId) {
        Object id = entityManager
                .createNativeQuery("SELECT id FROM listings WHERE unit_id = ? AND status = 'ACTIVE'")
                .setParameter(1, unitId)
                .getSingleResult();
        return UUID.fromString(id.toString());
    }

    private UUID seedListedUnitAndListing() throws Exception {
        userTransaction.begin();
        try {
            UUID modelId = (UUID) entityManager
                    .createNativeQuery("SELECT id FROM product_models LIMIT 1")
                    .getSingleResult();

            UUID unitId = UUID.randomUUID();
            entityManager.createNativeQuery("""
                            INSERT INTO product_units (id, product_model_id, serial_number, current_owner_id,
                                current_custody, status, version, created_at, updated_at)
                            VALUES (?, ?, ?, 777, 'SELLER', 'LISTED', 0, now(), now())
                            """)
                    .setParameter(1, unitId)
                    .setParameter(2, modelId)
                    .setParameter(3, "SN-IT-" + UUID.randomUUID())
                    .executeUpdate();

            UUID listingId = UUID.randomUUID();
            entityManager.createNativeQuery("""
                            INSERT INTO listings (id, unit_id, seller_id, title, description, asking_price,
                                status, grade_snapshot, version, created_at, updated_at)
                            VALUES (?, ?, 777, 'IT Seeded Listing', 'integration test', 10000000.00,
                                'ACTIVE', 'A+', 0, now(), now())
                            """)
                    .setParameter(1, listingId)
                    .setParameter(2, unitId)
                    .executeUpdate();

            entityManager.flush();
            userTransaction.commit();
            return unitId;
        } catch (Exception e) {
            userTransaction.rollback();
            throw e;
        }
    }
}
