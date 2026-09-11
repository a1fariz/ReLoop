package com.reloop.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class UserDtos {
    public record KycSubmissionDto(
        @NotBlank @Size(max = 50) String documentType,
        @NotBlank @Size(max = 100) String documentReference,
        @NotBlank @Size(max = 50) String nationalId,
        @NotNull @Past LocalDate dateOfBirth
    ) {}

    public record KycReviewDto(
        @NotNull Boolean approved,
        @Size(max = 1000) String notes
    ) {}

    public record ProfileDto(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String role,
        boolean verified,
        String kycStatus,
        String address
    ) {}

    public record UpdateProfileDto(
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 30) String phoneNumber,
        String address
    ) {}

    public record KycStatusDto(
        Long userId,
        String kycStatus,
        String kycDocumentType,
        String kycDocumentReference,
        String nationalId,
        LocalDate dateOfBirth,
        Instant kycSubmittedAt,
        Instant kycVerifiedAt
    ) {}

    public record KycLogDto(
        UUID id,
        Long userId,
        String previousStatus,
        String newStatus,
        Long reviewedBy,
        String notes,
        Instant createdAt
    ) {}
}
