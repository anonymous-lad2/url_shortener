package com.happysat.url_shortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * @param url  validated as non-blank and well-formed URL by Bean Validation.
 *             Field name matches the JSON key the frontend sends: {"url": "..."}.
 */
public record ShortenRequest(
        @NotBlank(message = "URL must not be blank")
        @URL(message = "Must be a valid URL")
        String url,

        @Size(min = 3, max = 11, message = "Alias must be 3-11 characters")
        @Pattern(regexp = "[0-9a-zA-Z_-]+", message = "Alias can only contain letters, numbers, hyphens, underscores")
        String customAlias,

        @Future(message = "Expiry must be in the future")
        LocalDateTime expiresAt
) {}
