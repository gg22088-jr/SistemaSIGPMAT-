package com.hospital.gestion.chatbot;

import com.hospital.gestion.model.Cita;
import com.hospital.gestion.model.Departamento;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Paciente;
import com.hospital.gestion.model.Permiso;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.repository.CitaRepository;
import com.hospital.gestion.repository.DisponibilidadRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.IncidenciaRepository;
import com.hospital.gestion.repository.PermisoRepository;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.repository.VacacionRepository;
import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotServiceTest {

    private final TurnoRepository turnos = mock(TurnoRepository.class);
    private final PermisoRepository permisos = mock(PermisoRepository.class);
    private final CitaRepository citas = mock(CitaRepository.class);
    private final ChatbotService bot = new ChatbotService(turnos, mock(DisponibilidadRepository.class), mock(VacacionRepository.class),
            permisos, mock(IncidenciaRepository.class), citas, mock(EmpleadoRepository.class));

    private static Contexto ctx(String grupo, Integer empleado, Integer departamento) {
        return new Contexto(1, grupo, empleado, departamento);
    }

    private static Departamento depto(int id, String nombre) {
        Departamento d = new Departamento();
        d.setIdDepartamento(id);
        d.setNombreDept(nombre);
        return d;
    }

    private static Empleado empleado(int id, String nombre, String apellido, Departamento d) {
        Empleado e = new Empleado();
        e.setIdEmpleado(id);
        e.setNombre(nombre);
        e.setApellido(apellido);
        e.setDepartamento(d);
        return e;
    }

    private static Turno turno(LocalDate fecha, String estado, Departamento d) {
        Turno t = new Turno();
        t.setFecha(fecha);
        t.setHoraInicio(LocalTime.of(7, 0));
        t.setHoraFin(LocalTime.of(15, 0));
        t.setEstadoTurno(estado);
        t.setDepartamento(d);
        return t;
    }

    private static Permiso permiso(Empleado e) {
        Permiso p = new Permiso();
        p.setEmpleado(e);
        p.setTipoPermiso("Medico");
        p.setFechaInicio(LocalDateTime.now().plusDays(1));
        p.setFechaFin(LocalDateTime.now().plusDays(1).plusHours(4));
        p.setEstadoPer("Pendiente");
        return p;
    }

    private static Cita cita(String paciente, Empleado medico) {
        Paciente p = new Paciente();
        p.setNombre(paciente);
        p.setApellido("Prueba");
        Cita c = new Cita();
        c.setPaciente(p);
        c.setEmpleado(medico);
        c.setFecha(LocalDate.now());
        c.setHoraInicio(LocalTime.of(9, 0));
        c.setHoraFin(LocalTime.of(9, 30));
        c.setEstadoCita("Programada");
        return c;
    }

    @Test
    void elProximoTurnoEsElPrimeroNoCanceladoDesdeHoy() {
        LocalDate hoy = LocalDate.now();
        Departamento farmacia = depto(9, "Farmacia");
        when(turnos.findByEmpleado_IdEmpleado(5)).thenReturn(List.of(
                turno(hoy.minusDays(1), "Cumplido", farmacia),
                turno(hoy.plusDays(1), "Cancelado", farmacia),
                turno(hoy.plusDays(3), "Programado", farmacia)));

        String texto = bot.responder(ctx(Grupos.OPERATIVO, 5, 9), "¿Cuál es mi próximo turno?").texto();

        assertTrue(texto.contains("07:00") && texto.contains("15:00") && texto.contains("Farmacia"), texto);
        assertFalse(texto.contains("mañana"), "el turno de mañana está cancelado: " + texto);
    }

    @Test
    void sinEmpleadoVinculadoNoPuedeConsultarSusTurnos() {
        String texto = bot.responder(ctx(Grupos.OPERATIVO, null, null), "mi próximo turno").texto();
        assertTrue(texto.contains("vinculado"), texto);
    }

    @Test
    void unTrabajadorOperativoVeSoloSuTurnoDeHoy() {
        when(turnos.findByEmpleado_IdEmpleado(5)).thenReturn(List.of(turno(LocalDate.now(), "Programado", depto(9, "Farmacia"))));

        String texto = bot.responder(ctx(Grupos.OPERATIVO, 5, 9), "turnos de hoy").texto();

        assertTrue(texto.startsWith("Hoy trabajas de 07:00 a 15:00"), texto);
    }

    @Test
    void losPendientesDeAprobarSoloLosVenQuienesGestionan() {
        String texto = bot.responder(ctx(Grupos.OPERATIVO, 5, 9), "permisos pendientes").texto();
        assertTrue(texto.contains("No tienes acceso"), texto);
    }

    @Test
    void laJefaturaSoloVeLosPermisosPendientesDeSuDepartamento() {
        Empleado deMiArea = empleado(10, "Ana", "López", depto(2, "Pediatría"));
        Empleado deOtraArea = empleado(11, "Luis", "Torres", depto(5, "Ginecología"));
        when(permisos.findByEstadoPer("Pendiente")).thenReturn(List.of(permiso(deMiArea), permiso(deOtraArea)));

        String texto = bot.responder(ctx(Grupos.JEFE, 3, 2), "permisos pendientes").texto();

        assertTrue(texto.contains("Ana López"), texto);
        assertFalse(texto.contains("Luis Torres"), texto);
        assertTrue(texto.contains("1 permiso pendiente"), texto);
    }

    @Test
    void elMedicoSoloVeSuAgendaDeHoy() {
        Empleado yo = empleado(7, "Roberto", "García", depto(3, "Cardiología"));
        Empleado otro = empleado(8, "María", "Hernández", depto(4, "Cirugía"));
        when(citas.findByFecha(LocalDate.now())).thenReturn(List.of(cita("Juan", yo), cita("Sofía", otro)));

        String texto = bot.responder(ctx(Grupos.MEDICO, 7, 3), "citas de hoy").texto();

        assertTrue(texto.contains("Juan"), texto);
        assertFalse(texto.contains("Sofía"), texto);
    }

    @Test
    void quienNoTieneAccesoALasCitasNoLasVe() {
        String texto = bot.responder(ctx(Grupos.OPERATIVO, 5, 9), "citas de hoy").texto();
        assertTrue(texto.contains("no tiene acceso"), texto);
    }

    @Test
    void laAyudaYLasSugerenciasDependenDelPerfil() {
        ChatbotService.Respuesta operativo = bot.responder(ctx(Grupos.OPERATIVO, 5, 9), "ayuda");
        ChatbotService.Respuesta jefatura = bot.responder(ctx(Grupos.JEFE, 3, 2), "ayuda");

        assertFalse(operativo.texto().contains("pendientes de aprobar"));
        assertTrue(jefatura.texto().contains("pendientes de aprobar"));
        assertFalse(operativo.sugerencias().contains("Permisos pendientes"));
        assertTrue(jefatura.sugerencias().contains("Permisos pendientes"));
    }

    @Test
    void unaPreguntaQueNoEntiendeSugiereOpciones() {
        ChatbotService.Respuesta r = bot.responder(ctx(Grupos.OPERATIVO, 5, 9), "blablabla");

        assertTrue(r.texto().contains("No entendí"), r.texto());
        assertFalse(r.sugerencias().isEmpty());
    }
}
