package com.hospital.gestion.controller;

import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController extends CrudController<Usuario, Integer> {

    private final PasswordEncoder encoder;

    public UsuarioController(UsuarioRepository repo, EntityManager em, PasswordEncoder encoder) {
        super(repo, em, "Usuario");
        this.encoder = encoder;
    }

    @Override
    protected void assignId(Usuario u, Integer id) {
        u.setIdUsuario(id);
    }

    @Override
    protected void beforeCreate(Usuario u) {
        if (u.getContrasena() == null || u.getContrasena().isBlank()) {
            throw new IllegalArgumentException("La contrasena es obligatoria");
        }
        u.setContrasena(encoder.encode(u.getContrasena()));
    }

    /** En PUT, si no se envía contrasena se conserva la actual; si se envía, se vuelve a cifrar. */
    @Override
    protected void beforeUpdate(Integer id, Usuario u) {
        if (u.getContrasena() == null || u.getContrasena().isBlank()) {
            u.setContrasena(find(id).getContrasena());
            em.clear();
        } else {
            u.setContrasena(encoder.encode(u.getContrasena()));
        }
    }
}
