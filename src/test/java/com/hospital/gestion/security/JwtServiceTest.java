package com.hospital.gestion.security;

import com.hospital.gestion.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String CLAVE = "clave-de-prueba-con-mas-de-32-caracteres-0123456789";
    private static final String OTRA_CLAVE = "otra-clave-distinta-con-mas-de-32-caracteres-9876543210";

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setNombreUsuario("admin01");
        u.setRol("Administrador");
        return u;
    }

    @Test
    void generaYLeeUnToken() {
        JwtService jwt = new JwtService(CLAVE, 60);
        Claims claims = jwt.leer(jwt.generar(usuario()));
        assertEquals("admin01", claims.getSubject());
        assertEquals("Administrador", claims.get("rol", String.class));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String token = new JwtService(OTRA_CLAVE, 60).generar(usuario());
        JwtService jwt = new JwtService(CLAVE, 60);
        assertThrows(JwtException.class, () -> jwt.leer(token));
    }

    @Test
    void rechazaUnTokenVencido() {
        JwtService jwt = new JwtService(CLAVE, -5);
        String token = jwt.generar(usuario());
        assertThrows(JwtException.class, () -> jwt.leer(token));
    }

    @Test
    void rechazaTextoQueNoEsUnToken() {
        JwtService jwt = new JwtService(CLAVE, 60);
        assertThrows(JwtException.class, () -> jwt.leer("esto-no-es-un-token"));
    }
}
