package com.hospital.gestion.chatbot;

import com.hospital.gestion.model.Cita;
import com.hospital.gestion.model.Disponibilidad;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Incidencia;
import com.hospital.gestion.model.Permiso;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.model.Vacacion;
import com.hospital.gestion.repository.CitaRepository;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.IncidenciaRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.VacacionRepository;
import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.security.Permisos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Asistente sencillo por palabras clave (sin IA ni servicios externos). Responde con datos del propio usuario y,
 * si su rol lo permite, con datos de gestión (pendientes, turnos y citas de hoy) dentro de su alcance.
 */
@Service
@Transactional(readOnly = true)
public class ChatbotService {

    public record Respuesta(String texto, List<String> sugerencias) { }

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("d/M HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final List<String> DIAS = List.of("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo");

    private static final String SIN_EMPLEADO = "Tu usuario no está vinculado a un empleado, por eso no puedo mostrarte tu información laboral. "
            + "Pide al administrador que lo vincule.";
    private static final String SIN_ACCESO = "No tienes acceso a esa información.";

    private final TurnoRepository turnos;
    private final DisponibilidadRepository disponibilidades;
    private final VacacionRepository vacaciones;
    private final PermisoRepository permisos;
    private final IncidenciaRepository incidencias;
    private final CitaRepository citas;
    private final EmpleadoRepository empleados;

    public ChatbotService(TurnoRepository turnos, DisponibilidadRepository disponibilidades, VacacionRepository vacaciones,
                          PermisoRepository permisos, IncidenciaRepository incidencias, CitaRepository citas,
                          EmpleadoRepository empleados) {
        this.turnos = turnos;
        this.disponibilidades = disponibilidades;
        this.vacaciones = vacaciones;
        this.permisos = permisos;
        this.incidencias = incidencias;
        this.citas = citas;
        this.empleados = empleados;
    }

    public Respuesta responder(Contexto ctx, String mensaje) {
        String texto = switch (Intenciones.detectar(mensaje)) {
            case COMO_SOLICITAR -> comoSolicitar(Intenciones.normalizar(mensaje));
            case CONTRASENA -> "Para cambiar tu contraseña entra a Mi espacio laboral → Mi cuenta → Cambiar contraseña. "
                    + "Debe tener al menos 8 caracteres, con letras y números, y ser distinta de la actual.";
            case VACACIONES_PENDIENTES -> vacacionesPendientes(ctx);
            case PERMISOS_PENDIENTES -> permisosPendientes(ctx);
            case CITAS -> citasHoy(ctx);
            case PROXIMO_TURNO -> proximoTurno(ctx);
            case TURNOS_HOY -> turnosHoy(ctx);
            case MIS_TURNOS -> misTurnos(ctx);
            case DISPONIBILIDAD -> miDisponibilidad(ctx);
            case VACACIONES -> misVacaciones(ctx);
            case PERMISOS -> misPermisos(ctx);
            case INCIDENCIAS -> misIncidencias(ctx);
            case PERFIL -> miPerfil(ctx);
            case AYUDA -> ayuda(ctx);
            case GRACIAS -> "¡De nada! Si necesitas algo más, aquí estoy.";
            case SALUDO -> "¡Hola! Soy el asistente de SIGPMAT. Pregúntame por tu próximo turno, tus vacaciones o tus permisos. "
                    + "Escribe «ayuda» para ver todo lo que puedo hacer.";
            case DESCONOCIDA -> "No entendí tu pregunta. Soy un asistente básico: entiendo preguntas sencillas sobre tu trabajo en el sistema. "
                    + "Prueba con una de las opciones de abajo o escribe «ayuda».";
        };
        return new Respuesta(texto, sugerencias(ctx));
    }

    // ---------------- información propia ----------------

    private String proximoTurno(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        LocalDate hoy = LocalDate.now();
        Optional<Turno> proximo = turnos.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .filter(t -> !"Cancelado".equals(t.getEstadoTurno()) && !t.getFecha().isBefore(hoy))
                .min(Comparator.comparing(Turno::getFecha).thenComparing(Turno::getHoraInicio));
        return proximo.map(t -> "Tu próximo turno es " + relativa(t.getFecha(), hoy) + ", de " + hh(t.getHoraInicio()) + " a "
                        + hh(t.getHoraFin()) + (t.getDepartamento() != null ? " en " + t.getDepartamento().getNombreDept() : "") + ".")
                .orElse("No tienes turnos programados por ahora.");
    }

    private String misTurnos(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        LocalDate hoy = LocalDate.now(), hasta = hoy.plusDays(14);
        List<Turno> lista = turnos.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .filter(t -> !"Cancelado".equals(t.getEstadoTurno()) && !t.getFecha().isBefore(hoy) && !t.getFecha().isAfter(hasta))
                .sorted(Comparator.comparing(Turno::getFecha).thenComparing(Turno::getHoraInicio)).toList();
        if (lista.isEmpty()) return "No tienes turnos en los próximos 14 días.";
        StringBuilder sb = new StringBuilder("Tus turnos de los próximos 14 días:\n");
        for (Turno t : lista.stream().limit(8).toList()) {
            sb.append("• ").append(relativa(t.getFecha(), hoy)).append(": ").append(hh(t.getHoraInicio())).append("–").append(hh(t.getHoraFin())).append('\n');
        }
        if (lista.size() > 8) sb.append("… y ").append(lista.size() - 8).append(" más. Los ves todos en Mis horarios y turnos.");
        return sb.toString().trim();
    }

    private String miDisponibilidad(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        List<Disponibilidad> lista = disponibilidades.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .sorted(Comparator.comparingInt((Disponibilidad d) -> DIAS.indexOf(d.getDiaSemana())).thenComparing(Disponibilidad::getHoraInicio)).toList();
        if (lista.isEmpty()) return "No tienes disponibilidad registrada.";
        StringBuilder sb = new StringBuilder("Tu disponibilidad semanal:\n");
        lista.forEach(d -> sb.append("• ").append(d.getDiaSemana()).append(": ").append(hh(d.getHoraInicio())).append("–").append(hh(d.getHoraFin())).append('\n'));
        return sb.toString().trim();
    }

    private String misVacaciones(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        List<Vacacion> lista = vacaciones.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .sorted(Comparator.comparing(Vacacion::getFechaInicio).reversed()).limit(5).toList();
        if (lista.isEmpty()) return "No tienes solicitudes de vacaciones. Puedes pedirlas en Mi espacio laboral → Mis vacaciones.";
        StringBuilder sb = new StringBuilder("Tus solicitudes de vacaciones más recientes:\n");
        lista.forEach(v -> sb.append("• Del ").append(v.getFechaInicio().format(FECHA)).append(" al ").append(v.getFechaFin().format(FECHA))
                .append(": ").append(v.getEstadoVac()).append('\n'));
        return sb.toString().trim();
    }

    private String misPermisos(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        List<Permiso> lista = permisos.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .sorted(Comparator.comparing(Permiso::getFechaInicio).reversed()).limit(5).toList();
        if (lista.isEmpty()) return "No tienes solicitudes de permisos. Puedes pedirlos en Mi espacio laboral → Mis permisos.";
        StringBuilder sb = new StringBuilder("Tus solicitudes de permisos más recientes:\n");
        lista.forEach(p -> sb.append("• ").append(p.getTipoPermiso()).append(", ").append(p.getFechaInicio().format(FECHA_HORA))
                .append(" a ").append(p.getFechaFin().format(FECHA_HORA)).append(": ").append(p.getEstadoPer()).append('\n'));
        return sb.toString().trim();
    }

    private String misIncidencias(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        List<Incidencia> lista = incidencias.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .sorted(Comparator.comparing(Incidencia::getIdIncidencia).reversed()).limit(5).toList();
        if (lista.isEmpty()) return "No tienes incidencias registradas.";
        StringBuilder sb = new StringBuilder("Tus incidencias más recientes:\n");
        lista.forEach(i -> sb.append("• ").append(i.getTipoIncidencia()).append(": ").append(i.getDescripcion()).append('\n'));
        return sb.toString().trim();
    }

    private String miPerfil(Contexto ctx) {
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        return empleados.findById(ctx.idEmpleado())
                .map(e -> "Eres " + nombre(e) + ", " + e.getCargo() + (e.getDepartamento() != null ? " del departamento " + e.getDepartamento().getNombreDept() : "")
                        + ". Más datos en Mi espacio laboral → Mi perfil.")
                .orElse(SIN_EMPLEADO);
    }

    private String comoSolicitar(String texto) {
        String vac = "Para solicitar vacaciones: entra a Mi espacio laboral → Mis vacaciones, pulsa «Solicitar vacaciones», elige las fechas y envía. "
                + "Quedará como Solicitado hasta que tu jefatura o Recursos Humanos la resuelvan, y puedes cancelarla mientras no esté resuelta.";
        String per = "Para solicitar un permiso: entra a Mi espacio laboral → Mis permisos, pulsa «Solicitar permiso», elige el tipo, las fechas y el motivo, y envía. "
                + "Quedará como Pendiente hasta que lo resuelvan, y puedes cancelarlo mientras no esté resuelto.";
        boolean pideVacaciones = texto.contains("vacacion"), pidePermiso = texto.contains("permiso");
        if (pideVacaciones && !pidePermiso) return vac;
        if (pidePermiso && !pideVacaciones) return per;
        return vac + "\n\n" + per;
    }

    // ---------------- gestión (según rol y alcance) ----------------

    private String turnosHoy(Contexto ctx) {
        LocalDate hoy = LocalDate.now();
        if (puedeLeer(ctx, "turnos")) {
            List<Turno> lista = turnos.findByFecha(hoy).stream()
                    .filter(t -> !"Cancelado".equals(t.getEstadoTurno()))
                    .filter(t -> !ctx.es(Grupos.JEFE) || (ctx.idDepartamento() != null && t.getDepartamento() != null
                            && ctx.idDepartamento().equals(t.getDepartamento().getIdDepartamento())))
                    .sorted(Comparator.comparing(Turno::getHoraInicio)).toList();
            if (lista.isEmpty()) return "Hoy no hay turnos programados" + (ctx.es(Grupos.JEFE) ? " en tu departamento." : ".");
            StringBuilder sb = new StringBuilder("Hoy hay " + lista.size() + (lista.size() == 1 ? " turno" : " turnos") + ":\n");
            for (Turno t : lista.stream().limit(6).toList()) {
                sb.append("• ").append(hh(t.getHoraInicio())).append("–").append(hh(t.getHoraFin())).append(" ").append(nombre(t.getEmpleado()))
                        .append(t.getDepartamento() != null ? " (" + t.getDepartamento().getNombreDept() + ")" : "").append('\n');
            }
            if (lista.size() > 6) sb.append("… y ").append(lista.size() - 6).append(" más. Los ves todos en la sección Turnos.");
            return sb.toString().trim();
        }
        if (ctx.idEmpleado() == null) return SIN_EMPLEADO;
        Optional<Turno> propio = turnos.findByEmpleado_IdEmpleado(ctx.idEmpleado()).stream()
                .filter(t -> !"Cancelado".equals(t.getEstadoTurno()) && hoy.equals(t.getFecha()))
                .min(Comparator.comparing(Turno::getHoraInicio));
        return propio.map(t -> "Hoy trabajas de " + hh(t.getHoraInicio()) + " a " + hh(t.getHoraFin())
                        + (t.getDepartamento() != null ? " en " + t.getDepartamento().getNombreDept() : "") + ".")
                .orElse("Hoy no tienes turno.");
    }

    private String permisosPendientes(Contexto ctx) {
        if (!puedeLeer(ctx, "permisos")) return SIN_ACCESO;
        List<Permiso> lista = permisos.findByEstadoPer("Pendiente").stream().filter(p -> enDepartamento(ctx, p.getEmpleado())).toList();
        if (lista.isEmpty()) return "No hay permisos pendientes de aprobar" + (ctx.es(Grupos.JEFE) ? " en tu departamento." : ".");
        StringBuilder sb = new StringBuilder("Hay " + lista.size() + (lista.size() == 1 ? " permiso pendiente" : " permisos pendientes") + " de aprobar:\n");
        for (Permiso p : lista.stream().limit(5).toList()) {
            sb.append("• ").append(nombre(p.getEmpleado())).append(": ").append(p.getTipoPermiso()).append(", ")
                    .append(p.getFechaInicio().format(FECHA_HORA)).append(" a ").append(p.getFechaFin().format(FECHA_HORA)).append('\n');
        }
        if (lista.size() > 5) sb.append("… y ").append(lista.size() - 5).append(" más. Los resuelves en el Panel principal o en la sección Permisos.");
        return sb.toString().trim();
    }

    private String vacacionesPendientes(Contexto ctx) {
        if (!puedeLeer(ctx, "vacaciones")) return SIN_ACCESO;
        List<Vacacion> lista = vacaciones.findByEstadoVac("Solicitado").stream().filter(v -> enDepartamento(ctx, v.getEmpleado())).toList();
        if (lista.isEmpty()) return "No hay solicitudes de vacaciones pendientes" + (ctx.es(Grupos.JEFE) ? " en tu departamento." : ".");
        StringBuilder sb = new StringBuilder("Hay " + lista.size() + (lista.size() == 1 ? " solicitud de vacaciones pendiente" : " solicitudes de vacaciones pendientes") + ":\n");
        for (Vacacion v : lista.stream().limit(5).toList()) {
            sb.append("• ").append(nombre(v.getEmpleado())).append(": del ").append(v.getFechaInicio().format(FECHA)).append(" al ")
                    .append(v.getFechaFin().format(FECHA)).append('\n');
        }
        if (lista.size() > 5) sb.append("… y ").append(lista.size() - 5).append(" más. Las resuelves en la sección Vacaciones.");
        return sb.toString().trim();
    }

    private String citasHoy(Contexto ctx) {
        if (!puedeLeer(ctx, "citas")) return "Tu perfil no tiene acceso a las citas.";
        boolean medico = ctx.es(Grupos.MEDICO);
        List<Cita> lista = citas.findByFecha(LocalDate.now()).stream()
                .filter(c -> !"Cancelada".equals(c.getEstadoCita()))
                .filter(c -> !medico || (ctx.idEmpleado() != null && c.getEmpleado() != null && ctx.idEmpleado().equals(c.getEmpleado().getIdEmpleado())))
                .sorted(Comparator.comparing(Cita::getHoraInicio)).toList();
        if (lista.isEmpty()) return medico ? "No tienes citas hoy." : "Hoy no hay citas programadas.";
        StringBuilder sb = new StringBuilder((medico ? "Tu agenda de hoy" : "Citas de hoy") + " (" + lista.size() + "):\n");
        for (Cita c : lista.stream().limit(6).toList()) {
            sb.append("• ").append(hh(c.getHoraInicio())).append("–").append(hh(c.getHoraFin())).append(" ")
                    .append(c.getPaciente() != null ? c.getPaciente().getNombre() + " " + c.getPaciente().getApellido() : "Paciente")
                    .append(medico ? "" : " con " + nombre(c.getEmpleado())).append(" (").append(c.getEstadoCita()).append(")\n");
        }
        if (lista.size() > 6) sb.append("… y ").append(lista.size() - 6).append(" más. Las ves todas en la sección Citas.");
        return sb.toString().trim();
    }

    // ---------------- ayuda y sugerencias ----------------

    private String ayuda(Contexto ctx) {
        StringBuilder sb = new StringBuilder("Soy un asistente básico: entiendo preguntas sencillas. Puedo ayudarte con:\n");
        sb.append("• Tu próximo turno y tus turnos de los próximos días\n");
        sb.append("• Tu disponibilidad semanal\n");
        sb.append("• El estado de tus vacaciones, permisos e incidencias\n");
        sb.append("• Cómo solicitar vacaciones o permisos, y cómo cambiar tu contraseña\n");
        if (puedeLeer(ctx, "permisos")) sb.append("• Permisos y vacaciones pendientes de aprobar\n");
        if (puedeLeer(ctx, "turnos")) sb.append("• Los turnos de hoy\n");
        if (puedeLeer(ctx, "citas")) sb.append("• Las citas de hoy\n");
        sb.append("Escribe tu pregunta con tus propias palabras.");
        return sb.toString();
    }

    private List<String> sugerencias(Contexto ctx) {
        List<String> s = new ArrayList<>(List.of("Mi próximo turno", "Mis vacaciones", "Mis permisos", "¿Cómo solicito vacaciones?"));
        if (puedeLeer(ctx, "permisos")) s.add("Permisos pendientes");
        if (puedeLeer(ctx, "turnos")) s.add("Turnos de hoy");
        if (puedeLeer(ctx, "citas")) s.add("Citas de hoy");
        s.add("Ayuda");
        return s;
    }

    // ---------------- utilidades ----------------

    private static boolean puedeLeer(Contexto ctx, String recurso) {
        return Boolean.TRUE.equals(Permisos.para(ctx.grupo()).get(recurso).get("leer"));
    }

    /** La jefatura solo ve su departamento; los demás grupos con acceso general ven todo. */
    private static boolean enDepartamento(Contexto ctx, Empleado e) {
        if (!ctx.es(Grupos.JEFE)) return true;
        return ctx.idDepartamento() != null && e != null && e.getDepartamento() != null
                && ctx.idDepartamento().equals(e.getDepartamento().getIdDepartamento());
    }

    private static String nombre(Empleado e) {
        return e == null ? "—" : e.getNombre() + " " + e.getApellido();
    }

    private static String hh(LocalTime t) {
        return t.format(HORA);
    }

    private static String relativa(LocalDate fecha, LocalDate hoy) {
        if (fecha.equals(hoy)) return "hoy";
        if (fecha.equals(hoy.plusDays(1))) return "mañana";
        String dia = DIA.format(fecha);
        return Character.toUpperCase(dia.charAt(0)) + dia.substring(1);
    }
}
