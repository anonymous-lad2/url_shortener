package com.happysat.url_shortener.service;

import com.happysat.url_shortener.exception.UrlNotFoundException;
import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.repository.UrlRepository;
import com.happysat.url_shortener.util.Base62Encoder;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UrlService {

    private final UrlRepository urlRepository;

    @Transactional
    public String shorten(String url) {
        ShortUrl res = new ShortUrl();
        res.setOriginalUrl(url);
        urlRepository.save(res);

        String shortUrl = Base62Encoder.encode(res.getId());
        res.setShortCode(shortUrl);
        urlRepository.save(res);

        return shortUrl;
    }

    public ShortUrl resolve(String code) {
        return urlRepository.findByShortCode(code)
                .orElseThrow(() -> new UrlNotFoundException(code));
    }
}
