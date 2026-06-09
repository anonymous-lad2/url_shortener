package com.happysat.url_shortener.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

/**
 * @param url  validated as non-blank and well-formed URL by Bean Validation.
 *             Field name matches the JSON key the frontend sends: {"url": "..."}.
 */
public record ShortenRequest(
        @NotBlank(message = "URL must not be blank")
        @URL(message = "Must be a valid URL")
        String url
) {}
