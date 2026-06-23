package com.happysat.url_shortener.service;

import com.happysat.url_shortener.dto.ClickMetadata;
import com.happysat.url_shortener.dto.StatsResponse;
import com.happysat.url_shortener.exception.UrlNotFoundException;
import com.happysat.url_shortener.model.ClickEvent;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.ClickEventRepository;
import com.happysat.url_shortener.repository.UrlRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final int MAX_HEADER_LENGTH = 512;

    private final UrlRepository urlRepository;
    private final ClickEventRepository clickEventRepository;

    @Async("clickEventExecutor")
    @Transactional
    public void recordClick(String code, ClickMetadata metadata) {
        urlRepository.findByShortCode(code).ifPresent(url -> {
            ClickEvent event = new ClickEvent();
            event.setShortUrlId(url.getId());
            event.setReferrer(truncate(metadata.referrer(), MAX_HEADER_LENGTH));
            event.setUserAgent(truncate(metadata.userAgent(), MAX_HEADER_LENGTH));
            event.setIpHash(hashIp(metadata.clientIp()));
            clickEventRepository.save(event);
        });
    }

    public StatsResponse getStats(String code) {
        ShortUrl shortUrl = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));

        long totalClicks = clickEventRepository.countByShortUrlId(shortUrl.getId());
        long uniqueVisitors = clickEventRepository.countDistinctIpHashByShortUrlId(shortUrl.getId());
        List<StatsResponse.ReferrerCount> topReferrers = clickEventRepository
                .findTopReferrers(shortUrl.getId())
                .stream()
                .map(row -> new StatsResponse.ReferrerCount(
                        (String) row[0],
                        ((Number) row[1]).longValue()))
                .toList();

        return new StatsResponse(
                code,
                shortUrl.getOriginalUrl(),
                totalClicks,
                uniqueVisitors,
                topReferrers
        );
    }

    private String truncate(String value, int maxLen) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLen ? value : value.substring(0, maxLen);
    }

    private String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(ip.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
