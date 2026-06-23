package com.happysat.url_shortener.dto;

import java.util.List;

public record StatsResponse(
        String shortCode,
        String originalUrl,
        long totalClicks,
        long uniqueVisitors,
        List<ReferrerCount> topReferrers
) {
    public record ReferrerCount(String referrer, long count) {}
}
