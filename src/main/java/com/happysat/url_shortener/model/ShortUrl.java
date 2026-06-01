package com.happysat.url_shortener.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.generator.EventType;

import java.time.LocalDateTime;

@Entity
@Table(name = "shorturl")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", length = 2048)
    private String originalUrl;

    @Column(name = "short_code", unique = true)
    private String shortCode;

    @CurrentTimestamp(event = EventType.INSERT)
    @Column(updatable = false)
    private LocalDateTime createdAt;

}
