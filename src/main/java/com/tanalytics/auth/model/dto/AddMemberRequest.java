package com.tanalytics.auth.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddMemberRequest(
        @Email @NotBlank String email,
        @NotBlank @Pattern(regexp = "admin|editor|viewer") String role
) {}

