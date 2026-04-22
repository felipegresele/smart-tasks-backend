package com.task.task.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${app.jwt.secret}") private String secret;
    @Value("${app.jwt.expiration}") private long expiration;

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extra, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(extra)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) { return extractClaim(token, Claims::getSubject); }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        return extractUsername(token).equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) { return extractExpiration(token).before(new Date()); }
    private Date extractExpiration(String token) { return extractClaim(token, Claims::getExpiration); }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody());
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}
