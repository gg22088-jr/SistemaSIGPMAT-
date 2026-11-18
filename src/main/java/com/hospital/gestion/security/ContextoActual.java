package com.hospital.gestion.security;

import com.hospital.gestion.exception.AccesoDenegadoException;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ContextoActual {

    private final UsuarioRepository usuarios;
    private final EmpleadoRepository empleados;

    public ContextoActual(UsuarioRepository usuarios, EmpleadoRepository empleados) {
        this.usuarios = usuarios;
        this.empleados = empleados;
    }

    /** Usuario autenticado + su grupo + el empleado ligado (Empleado.id_usuario) y el departamento de ese empleado. */
    public Contexto obtener() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            throw new AccesoDenegadoException("No autenticado");
        }
        Usuario u = usuarios.findByNombreUsuario(auth.getName())
                .orElseThrow(() -> new AccesoDenegadoException("Usuario no válido"));
        Empleado e = empleados.findFirstByUsuario_IdUsuario(u.getIdUsuario()).orElse(null);
        Integer departamento = (e != null && e.getDepartamento() != null) ? e.getDepartamento().getIdDepartamento() : null;
        return new Contexto(u.getIdUsuario(), Grupos.de(u.getRol()), e != null ? e.getIdEmpleado() : null, departamento);
    }
}
