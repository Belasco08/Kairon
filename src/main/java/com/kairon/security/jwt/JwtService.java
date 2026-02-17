package com.kairon.security.jwt;

import com.kairon.security.auth.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.refresh-secret}")
    private String refreshSecretKey;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /* =======================
       EXTRAÇÃO
    ======================= */

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject, false);
    }

    public String extractUsernameFromRefreshToken(String token) {
        return extractClaim(token, Claims::getSubject, true);
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> resolver,
            boolean refresh
    ) {
        return resolver.apply(extractAllClaims(token, refresh));
    }

    /* =======================
       GERAÇÃO
    ======================= */

    public String generateToken(UserDetails userDetails) {

        if (!(userDetails instanceof UserDetailsImpl user)) {
            throw new IllegalStateException("UserDetails inválido para geração de JWT");
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole());
        claims.put("companyId", user.getCompanyId());

        return buildToken(
                claims,
                user.getUsername(),
                jwtExpiration,
                secretKey
        );
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(
                Map.of(),
                userDetails.getUsername(),
                refreshExpiration,
                refreshSecretKey
        );
    }

    private String buildToken(
            Map<String, Object> claims,
            String subject,
            long expiration,
            String secret
    ) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(secret), SignatureAlgorithm.HS256)
                .compact();
    }

    /* =======================
       VALIDAÇÃO
    ======================= */

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isUserMatch = username.equals(userDetails.getUsername());
        boolean isExpired = isTokenExpired(token, false);

        if (!isUserMatch) System.out.println("⚠️ JWT User mismatch: Token=" + username + " vs DB=" + userDetails.getUsername());
        if (isExpired) System.out.println("⚠️ JWT Expired!");

        return (isUserMatch && !isExpired);
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        return extractUsernameFromRefreshToken(token).equals(userDetails.getUsername())
                && !isTokenExpired(token, true);
    }

    private boolean isTokenExpired(String token, boolean refresh) {
        return extractAllClaims(token, refresh)
                .getExpiration()
                .before(new Date());
    }

    /* =======================
       INTERNO
    ======================= */

    private Claims extractAllClaims(String token, boolean refresh) {
        String secret = refresh ? refreshSecretKey : secretKey;

        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey(secret))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey(String secret) {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }
}
