package com.happysat.url_shortener.repository;

import com.happysat.url_shortener.model.ShortUrl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UrlRepository")
class UrlRepositoryTest {

    @Autowired
    private UrlRepository urlRepository;

    @Test
    @DisplayName("findByShortCode returns entity when code exists")
    void findByShortCode_existing_returnsEntity() {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("abc");
        urlRepository.saveAndFlush(entity);

        Optional<ShortUrl> result = urlRepository.findByShortCode("abc");

        assertTrue(result.isPresent());
        assertEquals("https://google.com", result.get().getOriginalUrl());
        assertEquals("abc", result.get().getShortCode());
    }

    @Test
    @DisplayName("findByShortCode returns empty when code does not exist")
    void findByShortCode_nonExisting_returnsEmpty() {
        Optional<ShortUrl> result = urlRepository.findByShortCode("xyz");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("save generates auto-increment ID")
    void save_generatesId() {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        ShortUrl saved = urlRepository.save(entity);

        assertNotNull(saved.getId());
        assertTrue(saved.getId() > 0);
    }

    @Test
    @DisplayName("createdAt is populated automatically on insert")
    void save_populatesCreatedAt() {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("ts1");
        ShortUrl saved = urlRepository.saveAndFlush(entity);

        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("expiresAt is persisted when set")
    void save_persistsExpiresAt() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("exp1");
        entity.setExpiresAt(expiresAt);
        ShortUrl saved = urlRepository.saveAndFlush(entity);

        assertEquals(expiresAt, saved.getExpiresAt());
        assertEquals(expiresAt, urlRepository.findByShortCode("exp1").orElseThrow().getExpiresAt());
    }

    @Test
    @DisplayName("duplicate shortCode violates unique constraint")
    void shortCode_uniqueConstraint_throwsException() {
        ShortUrl first = new ShortUrl();
        first.setOriginalUrl("https://first.com");
        first.setShortCode("dup");
        urlRepository.saveAndFlush(first);

        ShortUrl second = new ShortUrl();
        second.setOriginalUrl("https://second.com");
        second.setShortCode("dup");

        assertThrows(DataIntegrityViolationException.class,
                () -> urlRepository.saveAndFlush(second));
    }
}
