package com.hospital.gestion.controller;

import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.security.JwtService;
import com.hospital.gestion.security.Permisos;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    public record LoginRequest(@NotBlank String nombreUsuario, @NotBlank String contrasena) { }

    private final UsuarioRepository usuarios;
    private final EmpleadoRepository empleados;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UsuarioRepository usuarios, EmpleadoRepository empleados,
                          PasswordEncoder encoder, JwtService jwt) {
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        Usuario u = usuarios.findByNombreUsuario(req.nombreUsuario()).orElse(null);
        String motivo = null;
        if (u == null) {
            motivo = "el usuario no existe";
        } else if (!"Activo".equals(u.getEstado())) {
            motivo = "el usuario está " + u.getEstado();
        } else if (!encoder.matches(req.contrasena(), u.getContrasena())) {
            String h = u.getContrasena();
            motivo = "la contraseña no coincide (hash guardado: " + h.substring(0, Math.min(7, h.length()))
                    + ", largo " + h.length() + "; contraseña recibida de " + req.contrasena().length() + " caracteres)";
        }
        if (motivo != null) log.warn("Login rechazado para '{}': {}", req.nombreUsuario(), motivo);
        if (motivo != null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("status", 401);
            error.put("mensaje", "Usuario o contraseña incorrectos");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        Map<String, Object> body = perfil(u);
        body.put("token", jwt.generar(u));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        Usuario u = usuarios.findByNombreUsuario(auth.getName()).orElse(null);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(perfil(u));
    }

    public record CambioContrasena(@NotBlank String actual,
                                   @NotBlank @Size(min = 8, max = 72, message = "La nueva contraseña debe tener entre 8 y 72 caracteres") String nueva) { }

    /** El usuario cambia SU contraseña (el usuario sale del token). No modifica ningún dato personal. */
    @PostMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(Authentication auth, @Valid @RequestBody CambioContrasena c) {
        Usuario u = usuarios.findByNombreUsuario(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (!encoder.matches(c.actual(), u.getContrasena())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta");
        }
        validarNueva(c.actual(), c.nueva(), u.getNombreUsuario());
        u.setContrasena(encoder.encode(c.nueva()));
        usuarios.save(u);
        log.info("El usuario '{}' cambió su contraseña", u.getNombreUsuario());
    }

    static void validarNueva(String actual, String nueva, String usuario) {
        if (nueva == null || nueva.length() < 8 || nueva.length() > 72) {
            throw new IllegalArgumentException("La nueva contraseña debe tener entre 8 y 72 caracteres");
        }
        if (!nueva.matches(".*[A-Za-z].*") || !nueva.matches(".*\\d.*")) {
            throw new IllegalArgumentException("La nueva contraseña debe incluir letras y números");
        }
        if (nueva.equals(actual)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser distinta de la actual");
        }
        if (nueva.equalsIgnoreCase(usuario)) {
            throw new IllegalArgumentException("La contraseña no puede ser igual al nombre de usuario");
        }
    }

    private Map<String, Object> perfil(Usuario u) {
        String grupo = Grupos.de(u.getRol());
        Map<String, Object> usuario = new LinkedHashMap<>();
        usuario.put("idUsuario", u.getIdUsuario());
        usuario.put("nombreUsuario", u.getNombreUsuario());
        usuario.put("rol", u.getRol());
        usuario.put("grupo", grupo);

        Map<String, Object> empleado = null;
        Empleado e = empleados.findFirstByUsuario_IdUsuario(u.getIdUsuario()).orElse(null);
        if (e != null) {
            empleado = new LinkedHashMap<>();
            empleado.put("idEmpleado", e.getIdEmpleado());
            empleado.put("nombre", e.getNombre());
            empleado.put("apellido", e.getApellido());
            empleado.put("cargo", e.getCargo());
            empleado.put("departamento", e.getDepartamento() != null ? e.getDepartamento().getNombreDept() : null);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usuario", usuario);
        body.put("empleado", empleado);
        body.put("permisos", Permisos.para(grupo));
        return body;
    }
}
