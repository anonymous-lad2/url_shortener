package com.happysat.url_shortener.controller;

import com.happysat.url_shortener.config.TestAsyncConfig;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.ClickEventRepository;
import com.happysat.url_shortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestAsyncConfig.class)
@Transactional
@DisplayName("Stats endpoint integration tests")
class StatsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlRepository urlRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Test
    @DisplayName("GET /api/stats/{code} returns click stats after redirects")
    void getStats_afterClicks_returnsAggregatedStats() throws Exception {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://example.com");
        entity.setShortCode("clickme");
        urlRepository.saveAndFlush(entity);

        mockMvc.perform(get("/clickme")
                        .header("Referer", "https://twitter.com")
                        .header("User-Agent", "Browser/1.0"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/clickme")
                        .header("Referer", "https://twitter.com")
                        .header("User-Agent", "Browser/2.0"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/clickme")
                        .header("Referer", "https://google.com")
                        .header("User-Agent", "Browser/3.0"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/api/stats/clickme"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("clickme"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.totalClicks").value(3))
                .andExpect(jsonPath("$.uniqueVisitors").value(1))
                .andExpect(jsonPath("$.topReferrers[0].referrer").value("https://twitter.com"))
                .andExpect(jsonPath("$.topReferrers[0].count").value(2))
                .andExpect(jsonPath("$.topReferrers[1].referrer").value("https://google.com"))
                .andExpect(jsonPath("$.topReferrers[1].count").value(1));
    }

    @Test
    @DisplayName("GET /api/stats/{code} returns zero counts when no clicks recorded")
    void getStats_noClicks_returnsZeroStats() throws Exception {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://example.com");
        entity.setShortCode("noclicks");
        urlRepository.saveAndFlush(entity);

        mockMvc.perform(get("/api/stats/noclicks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalClicks").value(0))
                .andExpect(jsonPath("$.uniqueVisitors").value(0))
                .andExpect(jsonPath("$.topReferrers").isEmpty());
    }

    @Test
    @DisplayName("GET /api/stats/{code} returns 404 for unknown code")
    void getStats_unknownCode_returns404() throws Exception {
        mockMvc.perform(get("/api/stats/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Short URL not found: unknown"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET unknown redirect does not record a click event")
    void redirect_unknownCode_doesNotRecordClick() throws Exception {
        mockMvc.perform(get("/missing-code"))
                .andExpect(status().isNotFound());

        assertEquals(0, clickEventRepository.count());
    }

    @Test
    @DisplayName("GET expired redirect does not record a click event")
    void redirect_expiredCode_doesNotRecordClick() throws Exception {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://example.com");
        entity.setShortCode("st-expired");
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        urlRepository.saveAndFlush(entity);

        mockMvc.perform(get("/st-expired")
                        .header("Referer", "https://google.com"))
                .andExpect(status().isGone());

        assertEquals(0, clickEventRepository.count());
    }
}
