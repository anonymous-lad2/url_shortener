package com.happysat.url_shortener.service;

import com.happysat.url_shortener.dto.ClickMetadata;
import com.happysat.url_shortener.dto.StatsResponse;
import com.happysat.url_shortener.exception.UrlNotFoundException;
import com.happysat.url_shortener.model.ClickEvent;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.ClickEventRepository;
import com.happysat.url_shortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService")
class AnalyticsServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    @DisplayName("recordClick() saves event with headers and hashed IP")
    void recordClick_existingCode_savesEvent() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setId(10L);
        shortUrl.setShortCode("abc");
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(shortUrl));

        analyticsService.recordClick("abc", new ClickMetadata(
                "https://google.com", "TestAgent/1.0", "192.168.1.1"));

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());
        ClickEvent saved = captor.getValue();
        assertEquals(10L, saved.getShortUrlId());
        assertEquals("https://google.com", saved.getReferrer());
        assertEquals("TestAgent/1.0", saved.getUserAgent());
        assertNotNull(saved.getIpHash());
        assertEquals(64, saved.getIpHash().length());
    }

    @Test
    @DisplayName("recordClick() does nothing when code is unknown")
    void recordClick_unknownCode_doesNotSave() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        analyticsService.recordClick("missing", new ClickMetadata(null, null, "127.0.0.1"));

        verify(clickEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordClick() truncates long header values")
    void recordClick_longHeaders_truncatesValues() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setId(1L);
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(shortUrl));
        String longValue = "x".repeat(600);

        analyticsService.recordClick("abc", new ClickMetadata(longValue, longValue, "10.0.0.1"));

        ArgumentCaptor<ClickEvent> captor = ArgumentCaptor.forClass(ClickEvent.class);
        verify(clickEventRepository).save(captor.capture());
        ClickEvent saved = captor.getValue();
        assertEquals(512, saved.getReferrer().length());
        assertEquals(512, saved.getUserAgent().length());
    }

    @Test
    @DisplayName("getStats() returns aggregated stats for existing code")
    void getStats_existingCode_returnsStats() {
        ShortUrl shortUrl = new ShortUrl();
        shortUrl.setId(5L);
        shortUrl.setOriginalUrl("https://example.com");
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(shortUrl));
        when(clickEventRepository.countByShortUrlId(5L)).thenReturn(3L);
        when(clickEventRepository.countDistinctIpHashByShortUrlId(5L)).thenReturn(2L);
        when(clickEventRepository.findTopReferrers(5L)).thenReturn(List.of(
                new Object[]{"https://twitter.com", 2L},
                new Object[]{"https://google.com", 1L}
        ));

        StatsResponse stats = analyticsService.getStats("abc");

        assertEquals("abc", stats.shortCode());
        assertEquals("https://example.com", stats.originalUrl());
        assertEquals(3, stats.totalClicks());
        assertEquals(2, stats.uniqueVisitors());
        assertEquals(2, stats.topReferrers().size());
        assertEquals("https://twitter.com", stats.topReferrers().get(0).referrer());
        assertEquals(2, stats.topReferrers().get(0).count());
    }

    @Test
    @DisplayName("getStats() throws UrlNotFoundException for unknown code")
    void getStats_unknownCode_throwsNotFound() {
        when(urlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        UrlNotFoundException ex = assertThrows(
                UrlNotFoundException.class,
                () -> analyticsService.getStats("missing")
        );
        assertTrue(ex.getMessage().contains("missing"));
    }
}
