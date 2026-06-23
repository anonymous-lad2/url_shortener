package com.happysat.url_shortener.repository;

import com.happysat.url_shortener.model.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByShortUrlId(Long shortUrlId);

    @Query("""
            SELECT COUNT(DISTINCT ce.ipHash)
            FROM ClickEvent ce
            WHERE ce.shortUrlId = :shortUrlId AND ce.ipHash IS NOT NULL
            """)
    long countDistinctIpHashByShortUrlId(@Param("shortUrlId") Long shortUrlId);

    @Query("""
            SELECT ce.referrer, COUNT(ce)
            FROM ClickEvent ce
            WHERE ce.shortUrlId = :shortUrlId AND ce.referrer IS NOT NULL
            GROUP BY ce.referrer
            ORDER BY COUNT(ce) DESC
            LIMIT 5
            """)
    List<Object[]> findTopReferrers(@Param("shortUrlId") Long shortUrlId);
}
