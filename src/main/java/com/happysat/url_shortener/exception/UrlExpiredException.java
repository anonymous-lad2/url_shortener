package com.happysat.url_shortener.exception;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String code) {
        super("Short URL has expired: " + code);
    }
}