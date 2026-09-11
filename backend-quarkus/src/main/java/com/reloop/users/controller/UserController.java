package com.reloop.users.controller;

import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import com.reloop.common.security.CurrentUser;
import com.reloop.users.dto.UserDtos;
import com.reloop.users.service.UserKycService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {
    private final UserKycService userKycService;
    private final CurrentUser currentUser;
    private final CorrelationContext correlationContext;

    @Inject
    public UserController(UserKycService userKycService, CurrentUser currentUser,
                         CorrelationContext correlationContext) {
        this.userKycService = userKycService;
        this.currentUser = currentUser;
        this.correlationContext = correlationContext;
    }

    @GET
    @Path("/me")
    public ApiResponse<UserDtos.ProfileDto> getMyProfile() {
        return ApiResponse.ok(userKycService.getProfile(currentUser.id()), correlationContext.getCorrelationId());
    }

    @PUT
    @Path("/me")
    public ApiResponse<UserDtos.ProfileDto> updateMyProfile(@Valid UserDtos.UpdateProfileDto request) {
        userKycService.updateProfile(currentUser.id(), request);
        return ApiResponse.ok(userKycService.getProfile(currentUser.id()), "Profile updated", correlationContext.getCorrelationId());
    }

    @POST
    @Path("/me/kyc")
    public RestResponse<ApiResponse<UserDtos.KycStatusDto>> submitMyKyc(@Valid UserDtos.KycSubmissionDto request) {
        userKycService.submitKyc(currentUser.id(), request);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(userKycService.getKycStatus(currentUser.id()), "KYC submitted", correlationContext.getCorrelationId()));
    }

    @GET
    @Path("/me/kyc")
    public ApiResponse<UserDtos.KycStatusDto> getMyKycStatus() {
        return ApiResponse.ok(userKycService.getKycStatus(currentUser.id()), correlationContext.getCorrelationId());
    }

    @GET
    @Path("/{id}/kyc")
    @RolesAllowed("ADMIN")
    public ApiResponse<UserDtos.KycStatusDto> getUserKycStatus(@PathParam("id") Long id) {
        return ApiResponse.ok(userKycService.getKycStatus(id), correlationContext.getCorrelationId());
    }

    @POST
    @Path("/{id}/kyc/review")
    @RolesAllowed("ADMIN")
    public RestResponse<ApiResponse<UserDtos.KycStatusDto>> reviewUserKyc(
            @PathParam("id") Long id,
            @Valid UserDtos.KycReviewDto request) {
        userKycService.reviewKyc(id, currentUser.id(), request);
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(userKycService.getKycStatus(id), "KYC review recorded", correlationContext.getCorrelationId()));
    }

    @GET
    @Path("/kyc/logs")
    @RolesAllowed("ADMIN")
    public ApiResponse<com.reloop.common.dto.Page<UserDtos.KycLogDto>> getKycLogs(
            @QueryParam("userId") Long userId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Long target = userId != null ? userId : currentUser.id();
        return ApiResponse.ok(userKycService.getKycLogs(target, Math.max(page, 0), safeSize),
                correlationContext.getCorrelationId());
    }
}
