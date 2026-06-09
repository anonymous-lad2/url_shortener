package com.happysat.url_shortener.service;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService")
class UrlServiceTest {

    @Mock
    private UrlRepository urlRepository;

    @InjectMocks
    private UrlService urlService;

    @Test
    @DisplayName("shorten() saves entity twice and returns Base62-encoded code")
    void shorten_savesAndReturnsCode() {
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(1L);
            }
            return entity;
        });

        String code = urlService.shorten("https://google.com");

        assertEquals(Base62Encoder.encode(1L), code);
        verify(urlRepository, times(2)).save(any(ShortUrl.class));
    }

    @Test
    @DisplayName("shorten() sets the shortCode on the entity before second save")
    void shorten_setsShortCodeOnEntity() {
        when(urlRepository.save(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl entity = invocation.getArgument(0);
            if (entity.getId() == null) {
                entity.setId(42L);
            }
            return entity;
        });

        String code = urlService.shorten("https://example.com");

        assertEquals(Base62Encoder.encode(42L), code);
    }

    @Test
    @DisplayName("resolve() returns original URL for existing code")
    void resolve_existingCode_returnsOriginalUrl() {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl("https://google.com");
        entity.setShortCode("abc");
        when(urlRepository.findByShortCode("abc")).thenReturn(Optional.of(entity));

        String result = urlService.resolve("abc");

        assertEquals("https://google.com", result);
        verify(urlRepository).findByShortCode("abc");
    }

    @Test
    @DisplayName("resolve() throws UrlNotFoundException for unknown code")
    void resolve_unknownCode_throwsNotFound() {
        when(urlRepository.findByShortCode("xyz")).thenReturn(Optional.empty());

        UrlNotFoundException ex = assertThrows(
                UrlNotFoundException.class,
                () -> urlService.resolve("xyz")
        );
        assertTrue(ex.getMessage().contains("xyz"));
    }
}
