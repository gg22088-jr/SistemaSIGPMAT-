package com.hospital.gestion.controller;

import com.hospital.gestion.model.Cita;
import com.hospital.gestion.model.Departamento;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Paciente;
import com.hospital.gestion.model.Reporte;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.model.Vacacion;
import com.hospital.gestion.repository.CitaRepository;
import com.hospital.gestion.repository.DepartamentoRepository;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.PacienteRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.ReporteRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.VacacionRepository;
import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.service.ReglasAgenda;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Alcance por fila: quién puede ver cada registro concreto. */
class AlcanceControllersTest {

    private final EntityManager em = mock(EntityManager.class);
    private final CitaRepository citaRepo = mock(CitaRepository.class);
    private final ReglasAgenda agenda = new ReglasAgenda(mock(TurnoRepository.class), mock(DisponibilidadRepository.class),
            mock(VacacionRepository.class), mock(PermisoRepository.class), em);

    private static Contexto ctx(String grupo, Integer empleado, Integer departamento) {
        return new Contexto(1, grupo, empleado, departamento);
    }

    private static Departamento depto(int id) {
        Departamento d = new Departamento();
        d.setIdDepartamento(id);
        return d;
    }

    private static Empleado empleado(int id, Departamento d, String rolUsuario) {
        Empleado e = new Empleado();
        e.setIdEmpleado(id);
        e.setDepartamento(d);
        if (rolUsuario != null) {
            Usuario u = new Usuario();
            u.setRol(rolUsuario);
            e.setUsuario(u);
        }
        return e;
    }

    // ---------- turnos ----------

    @Test
    void laJefaturaSoloVeLosTurnosDeSuDepartamento() {
        TurnoController c = new TurnoController(mock(TurnoRepository.class), em, agenda);
        Turno propio = new Turno();
        propio.setDepartamento(depto(3));
        Turno ajeno = new Turno();
        ajeno.setDepartamento(depto(4));

        assertTrue(c.enAlcance(ctx(Grupos.JEFE, 5, 3), propio));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 5, 3), ajeno));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 5, null), propio));
        assertTrue(c.enAlcance(ctx(Grupos.RRHH, null, null), ajeno));
        assertTrue(c.enAlcance(ctx(Grupos.DIRECTOR, null, null), ajeno));
        assertFalse(c.enAlcance(ctx(Grupos.MEDICO, 7, 3), propio));
        assertFalse(c.enAlcance(ctx(Grupos.OPERATIVO, 8, 3), propio));
    }

    // ---------- citas ----------

    @Test
    void elMedicoSoloVeSusCitas() {
        CitaController c = new CitaController(citaRepo, em, agenda);
        Cita suya = new Cita();
        suya.setEmpleado(empleado(7, depto(3), null));
        Cita deOtro = new Cita();
        deOtro.setEmpleado(empleado(8, depto(3), null));

        assertTrue(c.enAlcance(ctx(Grupos.MEDICO, 7, 3), suya));
        assertFalse(c.enAlcance(ctx(Grupos.MEDICO, 7, 3), deOtro));
        assertTrue(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), deOtro));
        assertTrue(c.enAlcance(ctx(Grupos.ADMIN, 1, 1), deOtro));
        assertTrue(c.enAlcance(ctx(Grupos.DIRECTOR, null, null), deOtro));
        assertFalse(c.enAlcance(ctx(Grupos.ENFERMERIA, 9, 3), suya));
        assertFalse(c.enAlcance(ctx(Grupos.OPERATIVO, 9, 3), suya));
    }

    // ---------- pacientes ----------

    @Test
    void elMedicoYEnfermeriaSoloVenPacientesRelacionados() {
        PacienteController c = new PacienteController(mock(PacienteRepository.class), em, citaRepo);
        Paciente suyo = new Paciente();
        suyo.setIdPaciente(5);
        Paciente ajeno = new Paciente();
        ajeno.setIdPaciente(6);
        when(citaRepo.existsByPaciente_IdPacienteAndEmpleado_IdEmpleado(5, 7)).thenReturn(true);
        when(citaRepo.existsByPaciente_IdPacienteAndDepartamento_IdDepartamento(5, 3)).thenReturn(true);

        assertTrue(c.enAlcance(ctx(Grupos.MEDICO, 7, 3), suyo));
        assertFalse(c.enAlcance(ctx(Grupos.MEDICO, 7, 3), ajeno));
        assertTrue(c.enAlcance(ctx(Grupos.ENFERMERIA, 9, 3), suyo));
        assertFalse(c.enAlcance(ctx(Grupos.ENFERMERIA, 9, 4), suyo));
        assertTrue(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), ajeno));
        assertTrue(c.enAlcance(ctx(Grupos.DIRECTOR, null, null), ajeno));
        assertFalse(c.enAlcance(ctx(Grupos.OPERATIVO, 9, 3), suyo));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 2, 3), suyo));
    }

    // ---------- vacaciones ----------

    @Test
    void laJefaturaSoloVeLasSolicitudesDeSuDepartamento() {
        VacacionController c = new VacacionController(mock(VacacionRepository.class), em);
        Vacacion deMiArea = new Vacacion();
        deMiArea.setEmpleado(empleado(10, depto(2), null));
        Vacacion deOtraArea = new Vacacion();
        deOtraArea.setEmpleado(empleado(11, depto(5), null));

        assertTrue(c.enAlcance(ctx(Grupos.JEFE, 3, 2), deMiArea));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 3, 2), deOtraArea));
        assertTrue(c.enAlcance(ctx(Grupos.RRHH, null, null), deOtraArea));
        assertFalse(c.enAlcance(ctx(Grupos.MEDICO, 3, 2), deMiArea));
    }

    // ---------- empleados ----------

    @Test
    void recepcionSoloVeEmpleadosQueAtiendenCitas() {
        EmpleadoController c = new EmpleadoController(mock(EmpleadoRepository.class), em);

        assertTrue(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), empleado(1, depto(3), "Pediatra")));
        assertTrue(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), empleado(2, depto(3), "Jefe de Área")));
        assertFalse(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), empleado(3, depto(9), "Farmacéutico")));
        assertFalse(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), empleado(4, depto(9), "Conserje")));
        assertTrue(c.enAlcance(ctx(Grupos.JEFE, 5, 3), empleado(1, depto(3), "Pediatra")));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 5, 3), empleado(3, depto(9), "Farmacéutico")));
        assertFalse(c.enAlcance(ctx(Grupos.OPERATIVO, 6, 9), empleado(3, depto(9), "Farmacéutico")));
    }

    // ---------- departamentos ----------

    @Test
    void laJefaturaSoloVeSuDepartamentoYRecepcionTodos() {
        DepartamentoController c = new DepartamentoController(mock(DepartamentoRepository.class), em);

        assertTrue(c.enAlcance(ctx(Grupos.JEFE, 5, 3), depto(3)));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 5, 3), depto(4)));
        assertTrue(c.enAlcance(ctx(Grupos.RECEPCION, 2, 8), depto(4)));
        assertFalse(c.enAlcance(ctx(Grupos.MEDICO, 7, 3), depto(3)));
    }

    // ---------- historial de reportes ----------

    @Test
    void cadaQuienVeSoloSusReportesSalvoAdministracionYDireccion() {
        ReporteController c = new ReporteController(mock(ReporteRepository.class), em);
        Usuario yo = new Usuario();
        yo.setIdUsuario(1);
        Usuario otro = new Usuario();
        otro.setIdUsuario(2);
        Reporte mio = new Reporte();
        mio.setUsuario(yo);
        Reporte ajeno = new Reporte();
        ajeno.setUsuario(otro);

        assertTrue(c.enAlcance(ctx(Grupos.JEFE, 5, 3), mio));
        assertFalse(c.enAlcance(ctx(Grupos.JEFE, 5, 3), ajeno));
        assertTrue(c.enAlcance(ctx(Grupos.DIRECTOR, null, null), ajeno));
        assertTrue(c.enAlcance(ctx(Grupos.ADMIN, 1, 1), ajeno));
    }
}
