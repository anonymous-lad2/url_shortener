package com.happysat.url_shortener.exception;

public class UrlNotFoundException extends RuntimeException {

    public UrlNotFoundException(String code) {
        super("Short URL not found: " + code);
    }
}
