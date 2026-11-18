package com.hospital.gestion.service;

import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Cita;
import com.hospital.gestion.model.Disponibilidad;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.VacacionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reglas de negocio de turnos y citas. Todos los errores son IllegalArgumentException
 * (el API los devuelve como 400 con el mensaje, y la página lo muestra).
 */
@Component
public class ReglasAgenda {

    private static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final TurnoRepository turnos;
    private final DisponibilidadRepository disponibilidades;
    private final VacacionRepository vacaciones;
    private final PermisoRepository permisos;
    private final EntityManager em;

    public ReglasAgenda(TurnoRepository turnos, DisponibilidadRepository disponibilidades,
                        VacacionRepository vacaciones, PermisoRepository permisos, EntityManager em) {
        this.turnos = turnos;
        this.disponibilidades = disponibilidades;
        this.vacaciones = vacaciones;
        this.permisos = permisos;
        this.em = em;
    }

    // ---------------- TURNOS ----------------

    /**
     * Un turno Programado no puede: asignarse a un empleado incapacitado, caer en vacaciones o permisos
     * aprobados, cruzarse con otro turno del mismo empleado, ni salirse de su disponibilidad semanal.
     * @param excluir id del turno que se está editando (null si es nuevo)
     */
    public void validarTurno(Turno t, Integer excluir) {
        if (!"Programado".equals(t.getEstadoTurno())) return;
        Integer emp = t.getEmpleado().getIdEmpleado();
        Object[] datos = datosEmpleado(emp);
        String quien = datos[0] + " " + datos[1];
        if ("Incapacitado".equals(datos[2])) {
            throw new IllegalArgumentException(quien + " está incapacitado/a y no puede recibir turnos");
        }
        LocalDateTime[] iv = intervalo(t.getFecha(), t.getHoraInicio(), t.getHoraFin());
        LocalDate ultimoDia = iv[1].minusSeconds(1).toLocalDate();

        if (vacaciones.hayVacacionAprobada(emp, iv[0].toLocalDate(), ultimoDia)) {
            throw new IllegalArgumentException(quien + " tiene vacaciones aprobadas en esas fechas");
        }
        if (permisos.hayPermisoAprobado(emp, iv[0], iv[1])) {
            throw new IllegalArgumentException(quien + " tiene un permiso aprobado que se cruza con ese horario");
        }
        int idExcluido = excluir == null ? -1 : excluir;
        for (Turno otro : turnos.turnosCercanos(emp, t.getFecha().minusDays(1), t.getFecha().plusDays(1), idExcluido)) {
            LocalDateTime[] ov = intervalo(otro.getFecha(), otro.getHoraInicio(), otro.getHoraFin());
            if (iv[0].isBefore(ov[1]) && ov[0].isBefore(iv[1])) {
                throw new IllegalArgumentException(quien + " ya tiene un turno que se cruza con ese horario ("
                        + otro.getFecha() + " " + otro.getHoraInicio().format(HORA) + "–" + otro.getHoraFin().format(HORA) + ")");
            }
        }
        validarDisponibilidad(emp, quien, iv[0], iv[1]);
    }

    // ---------------- CITAS ----------------

    /** El médico no puede estar de vacaciones ni con permiso aprobado, y la cita debe caer en su disponibilidad. */
    public void validarCita(Cita c) {
        Integer emp = c.getEmpleado().getIdEmpleado();
        Object[] datos = datosEmpleado(emp);
        String quien = datos[0] + " " + datos[1];
        LocalDateTime inicio = c.getFecha().atTime(c.getHoraInicio());
        LocalDateTime fin = c.getFecha().atTime(c.getHoraFin());

        if (vacaciones.hayVacacionAprobada(emp, c.getFecha(), c.getFecha())) {
            throw new IllegalArgumentException(quien + " tiene vacaciones aprobadas el " + c.getFecha());
        }
        if (permisos.hayPermisoAprobado(emp, inicio, fin)) {
            throw new IllegalArgumentException(quien + " tiene un permiso aprobado que se cruza con esa cita");
        }
        validarDisponibilidad(emp, quien, inicio, fin);
    }

    // ---------------- COMUNES ----------------

    /** Si el empleado no tiene disponibilidad registrada no hay restricción; si la tiene, el horario debe caber en una franja. */
    private void validarDisponibilidad(Integer emp, String quien, LocalDateTime inicio, LocalDateTime fin) {
        List<Disponibilidad> lista = disponibilidades.findByEmpleado_IdEmpleado(emp);
        if (lista.isEmpty()) return;
        LocalDate dia = inicio.toLocalDate();
        for (Disponibilidad d : lista) {
            // se revisa el día del horario y el anterior, por si la franja es nocturna (ej. 22:00–06:00)
            for (LocalDate x : new LocalDate[]{dia.minusDays(1), dia}) {
                if (!nombreDia(x).equals(d.getDiaSemana())) continue;
                LocalDateTime[] franja = intervalo(x, d.getHoraInicio(), d.getHoraFin());
                if (!inicio.isBefore(franja[0]) && !fin.isAfter(franja[1])) return;
            }
        }
        String resumen = lista.stream()
                .map(d -> d.getDiaSemana() + " " + d.getHoraInicio().format(HORA) + "–" + d.getHoraFin().format(HORA))
                .collect(Collectors.joining(", "));
        throw new IllegalArgumentException(quien + " no tiene disponibilidad para " + nombreDia(dia) + " "
                + inicio.toLocalTime().format(HORA) + "–" + fin.toLocalTime().format(HORA)
                + ". Disponibilidad registrada: " + resumen);
    }

    /** Intervalo real [inicio, fin): si la hora fin no es posterior a la de inicio, termina al día siguiente. */
    private static LocalDateTime[] intervalo(LocalDate fecha, LocalTime inicio, LocalTime fin) {
        LocalDateTime s = fecha.atTime(inicio);
        LocalDateTime e = fecha.atTime(fin);
        if (!fin.isAfter(inicio)) e = e.plusDays(1);
        return new LocalDateTime[]{s, e};
    }

    private static String nombreDia(LocalDate d) {
        return DIAS[d.getDayOfWeek().getValue() - 1];
    }

    /** [nombre, apellido, estadoLaboral] del empleado. */
    private Object[] datosEmpleado(Integer id) {
        if (id == null) throw new IllegalArgumentException("Debe indicar el id del empleado");
        List<Object[]> r = em.createQuery(
                "select e.nombre, e.apellido, e.estadoLaboral from Empleado e where e.idEmpleado = :id", Object[].class)
                .setParameter("id", id).getResultList();
        if (r.isEmpty()) throw new ResourceNotFoundException("Empleado con id " + id + " no encontrado");
        return r.get(0);
    }
}
