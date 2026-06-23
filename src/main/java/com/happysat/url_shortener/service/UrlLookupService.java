package com.happysat.url_shortener.service;

import com.happysat.url_shortener.dto.CachedShortUrl;
import com.happysat.url_shortener.exception.UrlNotFoundException;
import com.happysat.url_shortener.repository.UrlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UrlLookupService {

    private final UrlRepository urlRepository;

    @Cacheable(value = "urlEntities", key = "#code")
    public CachedShortUrl lookup(String code) {
        return urlRepository.findByShortCode(code)
                .map(entity -> new CachedShortUrl(entity.getOriginalUrl(), entity.getExpiresAt()))
                .orElseThrow(() -> new UrlNotFoundException(code));
    }
}
