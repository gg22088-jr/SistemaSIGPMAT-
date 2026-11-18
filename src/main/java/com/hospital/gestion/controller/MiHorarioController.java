package com.hospital.gestion.controller;

import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Disponibilidad;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Incidencia;
import com.hospital.gestion.model.Permiso;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.model.Vacacion;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.IncidenciaRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import com.hospital.gestion.repository.VacacionRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mi espacio laboral (autoservicio): cada persona consulta y solicita SOLO lo suyo.
 * El empleado se obtiene en el servidor, del usuario autenticado (Empleado.id_usuario); ningún endpoint
 * recibe un identificador de empleado y cualquiera que venga en un cuerpo se ignora.
 * Abierto a cualquier usuario con sesión iniciada.
 */
@RestController
@RequestMapping("/api/mi")
@Transactional(readOnly = true)
public class MiHorarioController {

    private static final List<String> DIAS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

    public record SolicitudVacacion(@NotNull LocalDate fechaInicio, @NotNull LocalDate fechaFin) { }

    public record SolicitudPermiso(@NotBlank @Pattern(regexp = "Medico|Personal|Duelo", message = "tipoPermiso debe ser: Medico, Personal o Duelo") String tipoPermiso,
                                   @NotNull LocalDateTime fechaInicio, @NotNull LocalDateTime fechaFin,
                                   @NotBlank String motivo) { }

    private final UsuarioRepository usuarios;
    private final EmpleadoRepository empleados;
    private final TurnoRepository turnos;
    private final DisponibilidadRepository disponibilidades;
    private final VacacionRepository vacaciones;
    private final PermisoRepository permisos;
    private final IncidenciaRepository incidencias;

    public MiHorarioController(UsuarioRepository usuarios, EmpleadoRepository empleados, TurnoRepository turnos,
                               DisponibilidadRepository disponibilidades, VacacionRepository vacaciones,
                               PermisoRepository permisos, IncidenciaRepository incidencias) {
        this.usuarios = usuarios;
        this.empleados = empleados;
        this.turnos = turnos;
        this.disponibilidades = disponibilidades;
        this.vacaciones = vacaciones;
        this.permisos = permisos;
        this.incidencias = incidencias;
    }

    // ---------------- Mi perfil ----------------

    @GetMapping("/perfil")
    public Map<String, Object> perfil(Authentication auth) {
        Empleado yo = empleadoActual(auth);
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("nombre", yo.getNombre());
        p.put("apellido", yo.getApellido());
        p.put("cargo", yo.getCargo());
        p.put("departamento", yo.getDepartamento() != null ? yo.getDepartamento().getNombreDept() : null);
        p.put("especialidad", yo.getEspecialidad() != null ? yo.getEspecialidad().getNombreEspecialidad() : null);
        p.put("correo", yo.getCorreo());
        p.put("telefono", yo.getTelefono());
        return p;
    }

    // ---------------- Mis horarios y turnos ----------------

    /** Mis turnos entre desde y hasta (por defecto: de hoy a 30 días). */
    @GetMapping("/turnos")
    public List<Turno> misTurnos(Authentication auth,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Empleado yo = empleadoActual(auth);
        final LocalDate inicio = desde != null ? desde : LocalDate.now();
        final LocalDate fin = hasta != null ? hasta : inicio.plusDays(30);
        return turnos.findByEmpleado_IdEmpleado(yo.getIdEmpleado()).stream()
                .filter(t -> !t.getFecha().isBefore(inicio) && !t.getFecha().isAfter(fin))
                .sorted(Comparator.comparing(Turno::getFecha).thenComparing(Turno::getHoraInicio))
                .toList();
    }

    @GetMapping("/disponibilidad")
    public List<Disponibilidad> miDisponibilidad(Authentication auth) {
        Empleado yo = empleadoActual(auth);
        return disponibilidades.findByEmpleado_IdEmpleado(yo.getIdEmpleado()).stream()
                .sorted(Comparator.comparingInt((Disponibilidad d) -> DIAS.indexOf(d.getDiaSemana()))
                        .thenComparing(Disponibilidad::getHoraInicio))
                .toList();
    }

    // ---------------- Mis vacaciones ----------------

    @GetMapping("/vacaciones")
    public List<Vacacion> misVacaciones(Authentication auth) {
        Empleado yo = empleadoActual(auth);
        return vacaciones.findByEmpleado_IdEmpleado(yo.getIdEmpleado()).stream()
                .sorted(Comparator.comparing(Vacacion::getFechaInicio).reversed())
                .toList();
    }

    /** El empleado y el estado ('Solicitado') los fija el servidor. */
    @PostMapping("/vacaciones")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Vacacion solicitarVacaciones(Authentication auth, @Valid @RequestBody SolicitudVacacion s) {
        Empleado yo = empleadoActual(auth);
        if (s.fechaFin().isBefore(s.fechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (s.fechaInicio().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden solicitar vacaciones en fechas pasadas");
        }
        boolean seCruza = vacaciones.findByEmpleado_IdEmpleado(yo.getIdEmpleado()).stream()
                .filter(v -> !"Rechazado".equals(v.getEstadoVac()))
                .anyMatch(v -> !s.fechaInicio().isAfter(v.getFechaFin()) && !s.fechaFin().isBefore(v.getFechaInicio()));
        if (seCruza) {
            throw new IllegalArgumentException("Ya tiene una solicitud de vacaciones que se cruza con esas fechas");
        }
        Vacacion v = new Vacacion();
        v.setEmpleado(yo);
        v.setFechaInicio(s.fechaInicio());
        v.setFechaFin(s.fechaFin());
        v.setEstadoVac("Solicitado");
        return vacaciones.save(v);
    }

    /** Solo se puede cancelar una solicitud propia que aún no ha sido resuelta. */
    @DeleteMapping("/vacaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cancelarVacaciones(Authentication auth, @PathVariable Integer id) {
        Empleado yo = empleadoActual(auth);
        Vacacion v = vacaciones.findById(id)
                .filter(x -> x.getEmpleado() != null && yo.getIdEmpleado().equals(x.getEmpleado().getIdEmpleado()))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!"Solicitado".equals(v.getEstadoVac())) {
            throw new IllegalArgumentException("Solo se pueden cancelar solicitudes que aún no han sido resueltas");
        }
        vacaciones.delete(v);
    }

    // ---------------- Mis permisos ----------------

    @GetMapping("/permisos")
    public List<Permiso> misPermisos(Authentication auth) {
        Empleado yo = empleadoActual(auth);
        return permisos.findByEmpleado_IdEmpleado(yo.getIdEmpleado()).stream()
                .sorted(Comparator.comparing(Permiso::getFechaInicio).reversed())
                .toList();
    }

    /** El empleado y el estado ('Pendiente') los fija el servidor. */
    @PostMapping("/permisos")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Permiso solicitarPermiso(Authentication auth, @Valid @RequestBody SolicitudPermiso s) {
        Empleado yo = empleadoActual(auth);
        if (s.fechaFin().isBefore(s.fechaInicio())) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
        if (s.fechaInicio().toLocalDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se pueden solicitar permisos en fechas pasadas");
        }
        Permiso p = new Permiso();
        p.setEmpleado(yo);
        p.setTipoPermiso(s.tipoPermiso());
        p.setFechaInicio(s.fechaInicio());
        p.setFechaFin(s.fechaFin());
        p.setMotivo(s.motivo().trim());
        p.setEstadoPer("Pendiente");
        return permisos.save(p);
    }

    @DeleteMapping("/permisos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cancelarPermiso(Authentication auth, @PathVariable Integer id) {
        Empleado yo = empleadoActual(auth);
        Permiso p = permisos.findById(id)
                .filter(x -> x.getEmpleado() != null && yo.getIdEmpleado().equals(x.getEmpleado().getIdEmpleado()))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud no encontrada"));
        if (!"Pendiente".equals(p.getEstadoPer())) {
            throw new IllegalArgumentException("Solo se pueden cancelar solicitudes que aún no han sido resueltas");
        }
        permisos.delete(p);
    }

    // ---------------- Mis incidencias ----------------

    @GetMapping("/incidencias")
    public List<Incidencia> misIncidencias(Authentication auth) {
        Empleado yo = empleadoActual(auth);
        return incidencias.findByEmpleado_IdEmpleado(yo.getIdEmpleado()).stream()
                .sorted(Comparator.comparing(Incidencia::getIdIncidencia).reversed())
                .toList();
    }

    private Empleado empleadoActual(Authentication auth) {
        Usuario u = usuarios.findByNombreUsuario(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return empleados.findFirstByUsuario_IdUsuario(u.getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Su usuario no está vinculado a un empleado. Pida al administrador que lo vincule."));
    }
}
