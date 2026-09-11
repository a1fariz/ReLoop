package com.reloop.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Replaces the legacy Spring Modulith boundary verification: encodes the module
 * dependency graph as ArchUnit rules. Deliberate orchestrations are documented
 * at each rule (disputes/orders settle escrow, listings reference units, etc.).
 */
@AnalyzeClasses(packages = "com.reloop", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryArchitectureTest {

    // NOTE: common <-> auth is a sanctioned bidirectional coupling inherited from the
    // legacy Modulith design (common's JwtAuthenticationMechanism uses auth's JwtService),
    // so a blanket "no cycles" slice rule would flag it; the rules below encode the
    // real boundary constraints instead.
    @ArchTest
    static final ArchRule onlyCommonDependsOnAuth =
            noClasses().that().resideInAnyPackage("..catalog..", "..units..", "..listings..", "..orders..",
                            "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..",
                            "..disputes..", "..checkout..", "..outbox..", "..audit..")
                    .should().dependOnClassesThat().resideInAPackage("..auth..");

    @ArchTest
    static final ArchRule unitsIsLeaf = noClasses().that().resideInAPackage("..units..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..listings..", "..orders..",
                    "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..disputes..",
                    "..checkout..", "..outbox..", "..audit..", "..retention..");

    // listings may reference units (create/pause/resume mutate unit state)
    @ArchTest
    static final ArchRule listingsDependencies = noClasses().that().resideInAPackage("..listings..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..orders..",
                    "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..disputes..",
                    "..checkout..", "..outbox..", "..audit..", "..retention..");

    // orders orchestrates escrow settlement (ledger), warranty issuance, unit
    // ownership/custody transfer at completion, the shared outbox sink and the
    // audit trail
    @ArchTest
    static final ArchRule ordersDependencies = noClasses().that().resideInAPackage("..orders..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..listings..",
                    "..sellers..", "..tradein..", "..inspections..", "..disputes..", "..checkout..", "..retention..");

    @ArchTest
    static final ArchRule ledgerIsLeaf = noClasses().that().resideInAPackage("..ledger..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..orders..", "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..disputes..",
                    "..checkout..", "..outbox..", "..audit..", "..retention..");

    @ArchTest
    static final ArchRule outboxIsLeaf = noClasses().that().resideInAPackage("..outbox..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..orders..", "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..",
                    "..disputes..", "..checkout..", "..audit..", "..retention..");

    @ArchTest
    static final ArchRule auditIsLeaf = noClasses().that().resideInAPackage("..audit..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..orders..", "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..",
                    "..disputes..", "..checkout..", "..outbox..", "..retention..");

    // auth only depends on common
    @ArchTest
    static final ArchRule authDependsOnlyOnCommon = noClasses().that().resideInAPackage("..auth..")
            .should().dependOnClassesThat().resideInAnyPackage("..catalog..", "..units..", "..listings..", "..orders..",
                    "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..disputes..",
                    "..checkout..", "..outbox..", "..audit..", "..retention..");

    // inspections certify units: may update product units, nothing else beyond common
    @ArchTest
    static final ArchRule inspectionsDependencies = noClasses().that().resideInAPackage("..inspections..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..listings..", "..orders..",
                    "..ledger..", "..sellers..", "..tradein..", "..warranties..", "..disputes..", "..checkout..",
                    "..outbox..", "..audit..", "..retention..");

    // seller metrics aggregate real fulfillment + dispute + review data
    @ArchTest
    static final ArchRule sellersDependencies = noClasses().that().resideInAPackage("..sellers..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..ledger..", "..tradein..", "..inspections..", "..warranties..", "..checkout..", "..outbox..",
                    "..audit..", "..retention..");

    // reviews verify ownership through orders, nothing else beyond common
    @ArchTest
    static final ArchRule reviewsDependencies = noClasses().that().resideInAPackage("..reviews..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..disputes..",
                    "..checkout..", "..outbox..", "..audit..", "..retention..");

    // trade-in intake emits outbox events, nothing else beyond common
    @ArchTest
    static final ArchRule tradeinDependencies = noClasses().that().resideInAPackage("..tradein..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..orders..", "..ledger..", "..sellers..", "..inspections..", "..warranties..", "..disputes..",
                    "..checkout..", "..audit..", "..retention..");

    // disputes settle escrow: it may orchestrate orders + ledger and emit events
    // through the shared outbox sink, but nothing else.
    @ArchTest
    static final ArchRule disputesDependencies = noClasses().that().resideInAPackage("..disputes..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..units..", "..listings..",
                    "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..checkout..", "..audit..");

    // checkout orchestrates the allowed core modules plus the outbox sink
    @ArchTest
    static final ArchRule checkoutAllowedDependencies = noClasses().that().resideInAPackage("..checkout..")
            .should().dependOnClassesThat().resideInAnyPackage("..auth..", "..catalog..", "..sellers..", "..tradein..",
                    "..inspections..", "..warranties..", "..disputes..", "..audit..");

    // retention purges cross-module append-only tables
    @ArchTest
    static final ArchRule retentionDependencies = noClasses().that().resideInAPackage("..retention..")
            .should().dependOnClassesThat().resideInAnyPackage("..catalog..", "..units..", "..listings..", "..orders..",
                    "..ledger..", "..sellers..", "..tradein..", "..inspections..", "..warranties..", "..disputes..",
                    "..audit..");
}
