package com.happysat.url_shortener.dto;

import java.time.LocalDateTime;

public record ErrorResponse(String error, int status, LocalDateTime timestamp) {}
