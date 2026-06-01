package com.happysat.url_shortener.controller;

import com.happysat.url_shortener.model.ShortUrl;
import com.happysat.url_shortener.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/api/shorten")
    public ResponseEntity<?> shorten(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Field 'url' is required and must not be blank"));
        }

        String shortCode = urlService.shorten(url);
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(shortCode)
                .toUriString();

        return ResponseEntity.ok(Map.of("shortUrl", shortUrl));
    }

    @GetMapping("/{code:[0-9a-zA-Z]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        ShortUrl shortUrl = urlService.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(shortUrl.getOriginalUrl()))
                .build();
    }
}
