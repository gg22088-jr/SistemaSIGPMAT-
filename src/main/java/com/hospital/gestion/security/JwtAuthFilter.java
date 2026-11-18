package com.hospital.gestion.security;

import com.hospital.gestion.repository.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee el token Bearer, confirma que el usuario siga Activo y toma su rol actual de la base de datos.
 * (No es un @Component a propósito: se registra solo dentro de la cadena de Spring Security.)
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwt, UsuarioRepository usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.leer(header.substring(7));
                usuarios.findByNombreUsuario(claims.getSubject())
                        .filter(u -> "Activo".equals(u.getEstado()))
                        .ifPresent(u -> SecurityContextHolder.getContext().setAuthentication(
                                new UsernamePasswordAuthenticationToken(u.getNombreUsuario(), null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + Grupos.de(u.getRol()))))));
            } catch (JwtException | IllegalArgumentException ignorado) {
                // token inválido o vencido: sigue sin autenticar y la cadena responderá 401
            }
        }
        chain.doFilter(req, res);
    }
}
