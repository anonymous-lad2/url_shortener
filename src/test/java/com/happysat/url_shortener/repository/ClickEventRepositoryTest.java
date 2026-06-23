package com.happysat.url_shortener.repository;

import com.happysat.url_shortener.model.ClickEvent;
import com.happysat.url_shortener.model.ShortUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ClickEventRepository")
class ClickEventRepositoryTest {

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private UrlRepository urlRepository;

    private Long shortUrlId;

    @BeforeEach
    void setUp() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setOriginalUrl("https://example.com");
        shortUrl.setShortCode("stats-test");
        shortUrlId = urlRepository.saveAndFlush(shortUrl).getId();
    }

    @Test
    @DisplayName("countByShortUrlId returns click count for the given short URL")
    void countByShortUrlId_returnsCount() {
        saveClick("hash-a", "https://google.com");
        saveClick("hash-b", "https://twitter.com");

        assertEquals(2, clickEventRepository.countByShortUrlId(shortUrlId));
    }

    @Test
    @DisplayName("countDistinctIpHashByShortUrlId counts unique IP hashes only")
    void countDistinctIpHashByShortUrlId_countsUniqueHashes() {
        saveClick("same-hash", "https://google.com");
        saveClick("same-hash", "https://twitter.com");
        saveClick("other-hash", "https://reddit.com");

        assertEquals(2, clickEventRepository.countDistinctIpHashByShortUrlId(shortUrlId));
    }

    @Test
    @DisplayName("findTopReferrers returns referrers ordered by count descending")
    void findTopReferrers_ordersByCountDesc() {
        saveClick("hash-1", "https://twitter.com");
        saveClick("hash-2", "https://twitter.com");
        saveClick("hash-3", "https://google.com");

        List<Object[]> topReferrers = clickEventRepository.findTopReferrers(shortUrlId);

        assertEquals(2, topReferrers.size());
        assertEquals("https://twitter.com", topReferrers.get(0)[0]);
        assertEquals(2L, topReferrers.get(0)[1]);
        assertEquals("https://google.com", topReferrers.get(1)[0]);
        assertEquals(1L, topReferrers.get(1)[1]);
    }

    @Test
    @DisplayName("findTopReferrers excludes null referrers")
    void findTopReferrers_excludesNullReferrers() {
        saveClick("hash-1", null);
        saveClick("hash-2", "https://google.com");

        List<Object[]> topReferrers = clickEventRepository.findTopReferrers(shortUrlId);

        assertEquals(1, topReferrers.size());
        assertEquals("https://google.com", topReferrers.get(0)[0]);
    }

    private void saveClick(String ipHash, String referrer) {
        ClickEvent event = new ClickEvent();
        event.setShortUrlId(shortUrlId);
        event.setIpHash(ipHash);
        event.setReferrer(referrer);
        clickEventRepository.saveAndFlush(event);
    }
}
