package com.hospital.gestion.security;

import com.hospital.gestion.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracionMinutos;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generar(Usuario u) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(u.getNombreUsuario())
                .claim("rol", u.getRol())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMinutos * 60_000L))
                .signWith(key)
                .compact();
    }

    /** Lanza JwtException si el token es inválido o expiró. */
    public Claims leer(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
