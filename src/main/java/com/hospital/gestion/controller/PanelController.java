package com.hospital.gestion.controller;

import com.hospital.gestion.repository.CitaRepository;
import com.hospital.gestion.repository.PacienteRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Solo cifras agregadas (sin listas): el Director ve estadísticas de pacientes y citas sin acceder a los registros,
 * y el Administrador ve el estado de las cuentas de usuario. Acceso: Administrador y Director.
 */
@RestController
@RequestMapping("/api/panel")
@Transactional(readOnly = true)
public class PanelController {

    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final CitaRepository citas;

    public PanelController(UsuarioRepository usuarios, PacienteRepository pacientes, CitaRepository citas) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.citas = citas;
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas() {
        LocalDate hoy = LocalDate.now();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pacientes", pacientes.count());
        r.put("citasHoy", citas.contarEntre(hoy, hoy));
        r.put("citasMes", citas.contarEntre(hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth())));
        r.put("usuariosActivos", usuarios.countByEstado("Activo"));
        r.put("usuariosInactivos", usuarios.countByEstado("Inactivo"));
        r.put("usuariosSinEmpleado", usuarios.contarSinEmpleado());
        return r;
    }
}
