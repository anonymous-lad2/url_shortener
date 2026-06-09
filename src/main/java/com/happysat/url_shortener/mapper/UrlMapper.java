package com.happysat.url_shortener.mapper;

import com.happysat.url_shortener.dto.ShortenResponse;
import org.springframework.stereotype.Component;

@Component
public class UrlMapper {

    public ShortenResponse toResponse(String shortUrl, String originalUrl) {
        return new ShortenResponse(shortUrl, originalUrl);
    }
}
