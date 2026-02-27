package com.tanalytics.auth.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateSiteRequest(
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "^[a-zA-Z0-9.-]+$", message = "Invalid domain format") String domain
) {}

