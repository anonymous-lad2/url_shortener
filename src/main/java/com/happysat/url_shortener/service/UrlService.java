package com.happysat.url_shortener.service;

import com.happysat.url_shortener.dto.ShortenRequest;
import com.happysat.url_shortener.exception.AliasAlreadyExistsException;
import com.happysat.url_shortener.exception.UrlExpiredException;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.UrlRepository;
import com.happysat.url_shortener.util.Base62Encoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository urlRepository;
    private final UrlLookupService urlLookupService;

    @Transactional
    public String shorten(ShortenRequest request) {
        ShortUrl entity = new ShortUrl();
        entity.setOriginalUrl(request.url());
        entity.setExpiresAt(request.expiresAt());

        if(request.customAlias() != null) {
            if (urlRepository.findByShortCode(request.customAlias()).isPresent()) {
                throw new AliasAlreadyExistsException("Alias already exists: " + request.customAlias());
            }
            entity.setShortCode(request.customAlias());
            urlRepository.save(entity);  // one save — no ID needed for Base62
        }
        else {
            urlRepository.save(entity);  // get auto-generated ID
            entity.setShortCode(Base62Encoder.encode(entity.getId()));
            urlRepository.save(entity);  // update with encoded code
        }

        log.info("Shortened: {} -> {}", request.url(), entity.getShortCode());
        return entity.getShortCode();
    }

    public String resolve(String code) {
        var cached = urlLookupService.lookup(code);

        if (cached.expiresAt() != null && cached.expiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException(code);
        }

        log.info("Redirecting: {} -> {}", code, cached.originalUrl());
        return cached.originalUrl();
    }
}
