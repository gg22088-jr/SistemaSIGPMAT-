package com.hospital.gestion.controller;

import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import com.hospital.gestion.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EmpleadoRepository empleados = mock(EmpleadoRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService("clave-de-prueba-con-mas-de-32-caracteres-0123456789", 60);
    private final AuthController controlador = new AuthController(usuarios, empleados, encoder, jwt);
    private final Authentication auth = mock(Authentication.class);
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombreUsuario("farmaceutico01");
        usuario.setRol("Farmacéutico");
        usuario.setEstado("Activo");
        usuario.setContrasena(encoder.encode("Clave123"));
        when(auth.getName()).thenReturn("farmaceutico01");
        when(usuarios.findByNombreUsuario("farmaceutico01")).thenReturn(Optional.of(usuario));
    }

    private String mensajeAlCambiar(String actual, String nueva) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> controlador.cambiarContrasena(auth, new AuthController.CambioContrasena(actual, nueva)));
        return ex.getMessage();
    }

    @Test
    void elCambioCorrectoGuardaLaNuevaContrasenaCifrada() {
        controlador.cambiarContrasena(auth, new AuthController.CambioContrasena("Clave123", "NuevaClave9"));

        assertTrue(encoder.matches("NuevaClave9", usuario.getContrasena()));
        assertTrue(usuario.getContrasena().startsWith("$2"), "debe quedar cifrada con BCrypt");
        verify(usuarios).save(usuario);
    }

    @Test
    void despuesDelCambioSoloEntraConLaNuevaContrasena() {
        controlador.cambiarContrasena(auth, new AuthController.CambioContrasena("Clave123", "NuevaClave9"));

        ResponseEntity<Map<String, Object>> conNueva = controlador.login(new AuthController.LoginRequest("farmaceutico01", "NuevaClave9"));
        ResponseEntity<Map<String, Object>> conVieja = controlador.login(new AuthController.LoginRequest("farmaceutico01", "Clave123"));

        assertEquals(200, conNueva.getStatusCode().value());
        assertEquals(401, conVieja.getStatusCode().value());
    }

    @Test
    void laContrasenaActualDebeSerCorrecta() {
        assertTrue(mensajeAlCambiar("OtraClave1", "NuevaClave9").contains("actual"));
        verify(usuarios, never()).save(any());
    }

    @Test
    void laNuevaContrasenaDebeCumplirLasReglas() {
        assertTrue(mensajeAlCambiar("Clave123", "Ab1").contains("8"));                          // muy corta
        assertTrue(mensajeAlCambiar("Clave123", "SoloLetrasAqui").contains("letras y números")); // sin números
        assertTrue(mensajeAlCambiar("Clave123", "12345678").contains("letras y números"));      // sin letras
        assertTrue(mensajeAlCambiar("Clave123", "Clave123").contains("distinta"));              // igual a la actual
        assertTrue(mensajeAlCambiar("Clave123", "FARMACEUTICO01").contains("usuario"));         // igual al usuario
        verify(usuarios, never()).save(any());
    }
}
