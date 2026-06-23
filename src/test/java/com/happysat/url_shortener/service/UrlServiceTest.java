package com.happysat.url_shortener.service;

import com.happysat.url_shortener.dto.CachedShortUrl;
import com.happysat.url_shortener.dto.ShortenRequest;
import com.happysat.url_shortener.exception.AliasAlreadyExistsException;
import com.happysat.url_shortener.exception.UrlExpiredException;
import com.happysat.url_shortener.exception.UrlNotFoundException;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.UrlRepository;
import com.happysat.url_shortener.util.Base62Encoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService")
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UrlLookupService urlLookupService;

    @InjectMocks
    private UrlService urlService;

    @Test
    @DisplayName("shorten() without alias saves twice and returns Base62-encoded code")
    void shorten_withoutAlias_savesAndReturnsCode() {
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });

        String code = urlService.shorten(new ShortenRequest("https://google.com", null, null));

        assertEquals(Base62Encoder.encode(1L), code);
        verify(urlRepository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("shorten() with custom alias saves once and returns alias")
    void shorten_withCustomAlias_savesOnceAndReturnsAlias() {
        when(urlRepository.findByShortCode("my-link")).thenReturn(Optional.empty());
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String code = urlService.shorten(new ShortenRequest("https://google.com", "my-link", null));

        assertEquals("my-link", code);
        verify(urlRepository, times(1)).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("shorten() with taken alias throws AliasAlreadyExistsException")
    void shorten_withTakenAlias_throwsConflict() {
        ShortUrl existing = new ShortUrl();
        existing.setShortCode("taken");
        when(urlRepository.findByShortCode("taken")).thenReturn(Optional.of(existing));

        assertThrows(
                AliasAlreadyExistsException.class,
                () -> urlService.shorten(new ShortenRequest("https://google.com", "taken", null))
        );
        verify(urlRepository, never()).save(any());
    }

    @Test
    @DisplayName("shorten() stores expiresAt on entity")
    void shorten_withExpiresAt_setsExpiryOnEntity() {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(5L);
            }
            return entity;
        });

        urlService.shorten(new ShortenRequest("https://google.com", null, expiresAt));

        verify(urlRepository, times(2)).save(argThat(entity ->
                expiresAt.equals(entity.getExpiresAt())
        ));
    }

    @Test
    @DisplayName("resolve() returns original URL for existing code without expiry")
    void resolve_existingCodeWithoutExpiry_returnsOriginalUrl() {
        when(urlLookupService.lookup("abc")).thenReturn(new CachedShortUrl("https://google.com", null));

        String result = urlService.resolve("abc");

        assertEquals("https://google.com", result);
        verify(urlLookupService).lookup("abc");
    }

    @Test
    @DisplayName("resolve() returns original URL when expiry is in the future")
    void resolve_futureExpiry_returnsOriginalUrl() {
        when(urlLookupService.lookup("future")).thenReturn(
                new CachedShortUrl("https://google.com", LocalDateTime.now().plusDays(1)));

        String result = urlService.resolve("future");

        assertEquals("https://google.com", result);
    }

    @Test
    @DisplayName("resolve() throws UrlExpiredException when expiry is in the past")
    void resolve_pastExpiry_throwsGone() {
        when(urlLookupService.lookup("expired")).thenReturn(
                new CachedShortUrl("https://google.com", LocalDateTime.now().minusMinutes(1)));

        UrlExpiredException ex = assertThrows(
                UrlExpiredException.class,
                () -> urlService.resolve("expired")
        );
        assertTrue(ex.getMessage().contains("expired"));
    }

    @Test
    @DisplayName("resolve() throws UrlNotFoundException for unknown code")
    void resolve_unknownCode_throwsNotFound() {
        when(urlLookupService.lookup("xyz")).thenThrow(new UrlNotFoundException("xyz"));

        UrlNotFoundException ex = assertThrows(
                UrlNotFoundException.class,
                () -> urlService.resolve("xyz")
        );
        assertTrue(ex.getMessage().contains("xyz"));
    }
}
