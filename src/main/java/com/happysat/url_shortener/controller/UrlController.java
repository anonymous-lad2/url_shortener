package com.happysat.url_shortener.controller;

import com.happysat.url_shortener.dto.ClickMetadata;
import com.happysat.url_shortener.dto.ShortenRequest;
import com.happysat.url_shortener.dto.ShortenResponse;
import com.happysat.url_shortener.dto.StatsResponse;
import com.happysat.url_shortener.mapper.UrlMapper;
import com.happysat.url_shortener.service.AnalyticsService;
import com.happysat.url_shortener.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final UrlMapper urlMapper;
    private final AnalyticsService analyticsService;

    @PostMapping("/api/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        String shortCode = urlService.shorten(request);
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(shortCode)
                .toUriString();
        return ResponseEntity.ok(urlMapper.toResponse(shortUrl, request.url()));
    }

    @GetMapping("/{code:[0-9a-zA-Z_-]+}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        String originalUrl = urlService.resolve(code);
        analyticsService.recordClick(code, new ClickMetadata(
                request.getHeader("Referer"),
                request.getHeader("User-Agent"),
                request.getRemoteAddr()
        ));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl))
                .build();
    }

    @GetMapping("/api/stats/{code}")
    public StatsResponse getStats(@PathVariable String code) {
        return analyticsService.getStats(code);
    }
}
