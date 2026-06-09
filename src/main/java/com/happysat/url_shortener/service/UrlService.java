package com.happysat.url_shortener.service;

import com.happysat.url_shortener.exception.UrlNotFoundException;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.UrlRepository;
import com.happysat.url_shortener.util.Base62Encoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository urlRepository;

    @Transactional
    public String shorten(String url) {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl(url);
        urlRepository.save(entity);

        String shortCode = Base62Encoder.encode(entity.getId());
        entity.setShortCode(shortCode);
        urlRepository.save(entity);

        log.info("Shortened: {} -> {}", url, shortCode);
        return shortCode;
    }

    public String resolve(String code) {
        ShortUrl entity = urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));
        log.info("Redirecting: {} -> {}", code, entity.getOriginalUrl());
        return entity.getOriginalUrl();
    }
}
