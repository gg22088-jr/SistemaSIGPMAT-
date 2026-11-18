package com.hospital.gestion.service;

import com.hospital.gestion.model.Cita;
import com.hospital.gestion.model.Departamento;
import com.hospital.gestion.model.Disponibilidad;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.VacacionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Reglas de turnos y citas. El 9 de noviembre de 2026 es lunes. */
class ReglasAgendaTest {

    private final TurnoRepository turnos = mock(TurnoRepository.class);
    private final DisponibilidadRepository disponibilidades = mock(DisponibilidadRepository.class);
    private final VacacionRepository vacaciones = mock(VacacionRepository.class);
    private final PermisoRepository permisos = mock(PermisoRepository.class);
    private final EntityManager em = mock(EntityManager.class);
    private TypedQuery<Object[]> consulta;
    private ReglasAgenda agenda;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        consulta = mock(TypedQuery.class);
        when(em.createQuery(anyString(), eq(Object[].class))).thenReturn(consulta);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        when(consulta.getResultList()).thenReturn(Collections.singletonList(new Object[]{"Ana", "López", "Activo"}));
        agenda = new ReglasAgenda(turnos, disponibilidades, vacaciones, permisos, em);
    }

    // ---------- ayudas ----------

    private static Empleado empleado() {
        Empleado e = new Empleado();
        e.setIdEmpleado(1);
        return e;
    }

    private static Turno turno(String fecha, String inicio, String fin) {
        Departamento d = new Departamento();
        d.setIdDepartamento(1);
        Turno t = new Turno();
        t.setEmpleado(empleado());
        t.setDepartamento(d);
        t.setFecha(LocalDate.parse(fecha));
        t.setHoraInicio(LocalTime.parse(inicio));
        t.setHoraFin(LocalTime.parse(fin));
        t.setEstadoTurno("Programado");
        return t;
    }

    private static Disponibilidad disponibilidad(String dia, String inicio, String fin) {
        Disponibilidad d = new Disponibilidad();
        d.setEmpleado(empleado());
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.parse(inicio));
        d.setHoraFin(LocalTime.parse(fin));
        return d;
    }

    private static Cita cita(String fecha, String inicio, String fin) {
        Cita c = new Cita();
        c.setEmpleado(empleado());
        c.setFecha(LocalDate.parse(fecha));
        c.setHoraInicio(LocalTime.parse(inicio));
        c.setHoraFin(LocalTime.parse(fin));
        c.setEstadoCita("Programada");
        return c;
    }

    private static String mensaje(IllegalArgumentException e) {
        return e.getMessage() == null ? "" : e.getMessage();
    }

    // ---------- turnos ----------

    @Test
    void turnoDentroDeLaDisponibilidadSeAcepta() {
        when(disponibilidades.findByEmpleado_IdEmpleado(1))
                .thenReturn(List.of(disponibilidad("Lunes", "06:00", "22:00")));
        assertDoesNotThrow(() -> agenda.validarTurno(turno("2026-11-09", "06:00", "14:00"), null));
    }

    @Test
    void turnoFueraDeLaDisponibilidadSeRechaza() {
        when(disponibilidades.findByEmpleado_IdEmpleado(1))
                .thenReturn(List.of(disponibilidad("Martes", "06:00", "14:00")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarTurno(turno("2026-11-09", "06:00", "14:00"), null));
        assertTrue(mensaje(ex).contains("disponibilidad"));
    }

    @Test
    void sinDisponibilidadRegistradaNoHayRestriccion() {
        assertDoesNotThrow(() -> agenda.validarTurno(turno("2026-11-14", "07:00", "15:00"), null));
    }

    @Test
    void turnoNocturnoCabeEnUnaDisponibilidadNocturna() {
        when(disponibilidades.findByEmpleado_IdEmpleado(1))
                .thenReturn(List.of(disponibilidad("Lunes", "22:00", "06:00")));
        assertDoesNotThrow(() -> agenda.validarTurno(turno("2026-11-09", "22:00", "06:00"), null));
    }

    @Test
    void turnoEnVacacionesAprobadasSeRechaza() {
        when(vacaciones.hayVacacionAprobada(eq(1), any(), any())).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarTurno(turno("2026-11-09", "06:00", "14:00"), null));
        assertTrue(mensaje(ex).contains("vacaciones"));
    }

    @Test
    void turnoConPermisoAprobadoSeRechaza() {
        when(permisos.hayPermisoAprobado(eq(1), any(), any())).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarTurno(turno("2026-11-09", "06:00", "14:00"), null));
        assertTrue(mensaje(ex).contains("permiso"));
    }

    @Test
    void turnoQueSeCruzaConOtroSeRechaza() {
        when(turnos.turnosCercanos(eq(1), any(), any(), anyInt()))
                .thenReturn(List.of(turno("2026-11-09", "10:00", "12:00")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarTurno(turno("2026-11-09", "06:00", "14:00"), null));
        assertTrue(mensaje(ex).contains("se cruza"));
    }

    @Test
    void turnosConsecutivosSinSolaparseSeAceptan() {
        when(turnos.turnosCercanos(eq(1), any(), any(), anyInt()))
                .thenReturn(List.of(turno("2026-11-09", "06:00", "14:00")));
        assertDoesNotThrow(() -> agenda.validarTurno(turno("2026-11-09", "14:00", "22:00"), null));
    }

    @Test
    void empleadoIncapacitadoNoRecibeTurnos() {
        when(consulta.getResultList()).thenReturn(Collections.singletonList(new Object[]{"Pedro", "Díaz", "Incapacitado"}));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarTurno(turno("2026-11-09", "06:00", "14:00"), null));
        assertTrue(mensaje(ex).contains("incapacitado"));
    }

    @Test
    void unTurnoCanceladoNoSeValida() {
        when(vacaciones.hayVacacionAprobada(eq(1), any(), any())).thenReturn(true);
        Turno t = turno("2026-11-09", "06:00", "14:00");
        t.setEstadoTurno("Cancelado");
        assertDoesNotThrow(() -> agenda.validarTurno(t, null));
    }

    // ---------- citas ----------

    @Test
    void citaEnVacacionesAprobadasSeRechaza() {
        when(vacaciones.hayVacacionAprobada(eq(1), any(), any())).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarCita(cita("2026-11-18", "09:00", "09:30")));
        assertTrue(mensaje(ex).contains("vacaciones"));
    }

    @Test
    void citaFueraDeLaDisponibilidadSeRechaza() {
        when(disponibilidades.findByEmpleado_IdEmpleado(1))
                .thenReturn(List.of(disponibilidad("Martes", "07:00", "15:00")));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> agenda.validarCita(cita("2026-11-09", "09:00", "09:30")));
        assertTrue(mensaje(ex).contains("disponibilidad"));
    }

    @Test
    void citaDentroDeLaDisponibilidadSeAcepta() {
        when(disponibilidades.findByEmpleado_IdEmpleado(1))
                .thenReturn(List.of(disponibilidad("Lunes", "07:00", "15:00")));
        assertDoesNotThrow(() -> agenda.validarCita(cita("2026-11-09", "09:00", "09:30")));
    }
}
