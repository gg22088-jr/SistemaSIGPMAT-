package com.hospital.gestion.controller;

import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Disponibilidad;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.model.Vacacion;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.IncidenciaRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.VacacionRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiHorarioControllerTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final EmpleadoRepository empleados = mock(EmpleadoRepository.class);
    private final TurnoRepository turnos = mock(TurnoRepository.class);
    private final DisponibilidadRepository disponibilidades = mock(DisponibilidadRepository.class);
    private final VacacionRepository vacaciones = mock(VacacionRepository.class);
    private final PermisoRepository permisos = mock(PermisoRepository.class);
    private final IncidenciaRepository incidencias = mock(IncidenciaRepository.class);
    private final MiHorarioController controlador =
            new MiHorarioController(usuarios, empleados, turnos, disponibilidades, vacaciones, permisos, incidencias);
    private final Authentication auth = mock(Authentication.class);

    @BeforeEach
    void preparar() {
        when(auth.getName()).thenReturn("limpieza01");
        Usuario u = new Usuario();
        u.setIdUsuario(7);
        u.setNombreUsuario("limpieza01");
        when(usuarios.findByNombreUsuario("limpieza01")).thenReturn(Optional.of(u));
    }

    private Empleado vincularEmpleado() {
        Empleado e = new Empleado();
        e.setIdEmpleado(11);
        when(empleados.findFirstByUsuario_IdUsuario(7)).thenReturn(Optional.of(e));
        return e;
    }

    private static Turno turno(LocalDate fecha, String inicio) {
        Turno t = new Turno();
        t.setFecha(fecha);
        t.setHoraInicio(LocalTime.parse(inicio));
        t.setHoraFin(LocalTime.parse(inicio).plusHours(8));
        return t;
    }

    private static Disponibilidad disponibilidad(String dia, String inicio) {
        Disponibilidad d = new Disponibilidad();
        d.setDiaSemana(dia);
        d.setHoraInicio(LocalTime.parse(inicio));
        d.setHoraFin(LocalTime.parse(inicio).plusHours(8));
        return d;
    }

    @Test
    void soloDevuelveMisTurnosDeLosProximosDiasYOrdenados() {
        Empleado yo = vincularEmpleado();
        LocalDate hoy = LocalDate.now();
        when(turnos.findByEmpleado_IdEmpleado(yo.getIdEmpleado())).thenReturn(List.of(
                turno(hoy.plusDays(10), "06:00"),
                turno(hoy.minusDays(1), "06:00"),   // ya pasó
                turno(hoy.plusDays(40), "06:00"),   // fuera de los 30 días
                turno(hoy, "06:00")));

        List<Turno> mios = controlador.misTurnos(auth, null, null);

        assertEquals(2, mios.size());
        assertEquals(hoy, mios.get(0).getFecha());
        assertEquals(hoy.plusDays(10), mios.get(1).getFecha());
    }

    @Test
    void sinEmpleadoVinculadoSeRechaza() {
        when(empleados.findFirstByUsuario_IdUsuario(7)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> controlador.misTurnos(auth, null, null));
        assertTrue(ex.getMessage().contains("vinculado"));
    }

    @Test
    void miDisponibilidadSaleOrdenadaPorDiaDeLaSemana() {
        Empleado yo = vincularEmpleado();
        when(disponibilidades.findByEmpleado_IdEmpleado(yo.getIdEmpleado())).thenReturn(List.of(
                disponibilidad("Viernes", "06:00"),
                disponibilidad("Lunes", "06:00"),
                disponibilidad("Miércoles", "06:00")));

        List<Disponibilidad> mias = controlador.miDisponibilidad(auth);

        assertEquals("Lunes", mias.get(0).getDiaSemana());
        assertEquals("Miércoles", mias.get(1).getDiaSemana());
        assertEquals("Viernes", mias.get(2).getDiaSemana());
    }

    // ---------- vacaciones propias ----------

    private static Vacacion vacacion(Empleado e, LocalDate inicio, LocalDate fin, String estado) {
        Vacacion v = new Vacacion();
        v.setEmpleado(e);
        v.setFechaInicio(inicio);
        v.setFechaFin(fin);
        v.setEstadoVac(estado);
        return v;
    }

    @Test
    void lasVacacionesSolicitadasQuedanComoSolicitadoYDelEmpleadoAutenticado() {
        Empleado yo = vincularEmpleado();
        when(vacaciones.save(org.mockito.ArgumentMatchers.any(Vacacion.class))).thenAnswer(i -> i.getArgument(0));
        LocalDate inicio = LocalDate.now().plusDays(10);

        Vacacion creada = controlador.solicitarVacaciones(auth,
                new MiHorarioController.SolicitudVacacion(inicio, inicio.plusDays(5)));

        assertEquals("Solicitado", creada.getEstadoVac());
        assertEquals(yo.getIdEmpleado(), creada.getEmpleado().getIdEmpleado());
    }

    @Test
    void noSePuedenSolicitarVacacionesEnFechasPasadasNiConFechaFinalAnterior() {
        vincularEmpleado();
        LocalDate hoy = LocalDate.now();
        assertThrows(IllegalArgumentException.class, () -> controlador.solicitarVacaciones(auth,
                new MiHorarioController.SolicitudVacacion(hoy.minusDays(3), hoy.plusDays(2))));
        assertThrows(IllegalArgumentException.class, () -> controlador.solicitarVacaciones(auth,
                new MiHorarioController.SolicitudVacacion(hoy.plusDays(9), hoy.plusDays(5))));
    }

    @Test
    void noSePuedeSolicitarVacacionesQueSeCruzanConOtraSolicitudPropia() {
        Empleado yo = vincularEmpleado();
        LocalDate inicio = LocalDate.now().plusDays(20);
        when(vacaciones.findByEmpleado_IdEmpleado(yo.getIdEmpleado()))
                .thenReturn(List.of(vacacion(yo, inicio, inicio.plusDays(10), "Aprobado")));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> controlador.solicitarVacaciones(auth,
                new MiHorarioController.SolicitudVacacion(inicio.plusDays(5), inicio.plusDays(15))));
        assertTrue(ex.getMessage().contains("cruza"));
    }

    @Test
    void unaSolicitudRechazadaNoImpideSolicitarLasMismasFechas() {
        Empleado yo = vincularEmpleado();
        when(vacaciones.save(org.mockito.ArgumentMatchers.any(Vacacion.class))).thenAnswer(i -> i.getArgument(0));
        LocalDate inicio = LocalDate.now().plusDays(20);
        when(vacaciones.findByEmpleado_IdEmpleado(yo.getIdEmpleado()))
                .thenReturn(List.of(vacacion(yo, inicio, inicio.plusDays(10), "Rechazado")));

        Vacacion nueva = controlador.solicitarVacaciones(auth,
                new MiHorarioController.SolicitudVacacion(inicio, inicio.plusDays(10)));

        assertEquals("Solicitado", nueva.getEstadoVac());
    }

    @Test
    void soloSePuedeCancelarUnaSolicitudPropiaQueNoEstaResuelta() {
        Empleado yo = vincularEmpleado();
        Empleado otro = new Empleado();
        otro.setIdEmpleado(99);
        LocalDate inicio = LocalDate.now().plusDays(10);
        when(vacaciones.findById(1)).thenReturn(Optional.of(vacacion(yo, inicio, inicio.plusDays(2), "Aprobado")));
        when(vacaciones.findById(2)).thenReturn(Optional.of(vacacion(otro, inicio, inicio.plusDays(2), "Solicitado")));

        assertThrows(IllegalArgumentException.class, () -> controlador.cancelarVacaciones(auth, 1));   // ya resuelta
        assertThrows(ResourceNotFoundException.class, () -> controlador.cancelarVacaciones(auth, 2));  // de otra persona
    }
}
