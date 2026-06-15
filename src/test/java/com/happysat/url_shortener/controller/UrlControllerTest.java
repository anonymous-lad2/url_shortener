package com.happysat.url_shortener.controller;

import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.UrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("UrlController Integration Tests")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UrlRepository urlRepository;

    // ---- POST /api/shorten ----

    @Test
    @DisplayName("POST valid URL returns 200 with shortUrl and originalUrl")
    void shorten_validUrl_returns200() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://google.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").exists())
                .andExpect(jsonPath("$.originalUrl").value("https://google.com"));
    }

    @Test
    @DisplayName("POST with custom alias returns shortUrl containing alias")
    void shorten_customAlias_returns200WithAlias() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://google.com\", \"customAlias\": \"my-link\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortUrl").value(endsWith("/my-link")))
                .andExpect(jsonPath("$.originalUrl").value("https://google.com"));
    }

    @Test
    @DisplayName("POST duplicate custom alias returns 409")
    void shorten_duplicateAlias_returns409() throws Exception {
        ShortUrl existing = new ShortUrl();
        existing.setOriginalUrl("https://existing.com");
        existing.setShortCode("my-link");
        urlRepository.saveAndFlush(existing);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://google.com\", \"customAlias\": \"my-link\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Alias already exists: my-link"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST past expiresAt returns 400 validation error")
    void shorten_pastExpiresAt_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"https://google.com\", \"expiresAt\": \"2020-01-01T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("expiresAt: Expiry must be in the future"));
    }

    @Test
    @DisplayName("POST blank URL returns 400 with validation error")
    void shorten_blankUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST invalid URL returns 400")
    void shorten_invalidUrl_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\": \"not-a-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("POST missing url field returns 400")
    void shorten_missingField_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST malformed JSON returns 400")
    void shorten_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    @DisplayName("POST with no body returns 400")
    void shorten_noBody_returns400() throws Exception {
        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- GET /{code} ----

    @Test
    @DisplayName("GET existing code returns 302 with Location header")
    void redirect_existingCode_returns302() throws Exception {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("testcode");
        urlRepository.saveAndFlush(entity);

        mockMvc.perform(get("/testcode"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    @DisplayName("GET hyphenated custom alias returns 302")
    void redirect_hyphenatedAlias_returns302() throws Exception {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("my-link");
        urlRepository.saveAndFlush(entity);

        mockMvc.perform(get("/my-link"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://google.com"));
    }

    @Test
    @DisplayName("GET expired code returns 410")
    void redirect_expiredCode_returns410() throws Exception {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("expired");
        entity.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        urlRepository.saveAndFlush(entity);

        mockMvc.perform(get("/expired"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("Short URL has expired: expired"))
                .andExpect(jsonPath("$.status").value(410));
    }

    @Test
    @DisplayName("GET unknown code returns 404 with error body")
    void redirect_unknownCode_returns404() throws Exception {
        mockMvc.perform(get("/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.status").value(404));
    }
}
