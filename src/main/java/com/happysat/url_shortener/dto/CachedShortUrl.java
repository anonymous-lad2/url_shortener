package com.happysat.url_shortener.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CachedShortUrl(String originalUrl, LocalDateTime expiresAt) implements Serializable {}
