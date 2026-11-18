package com.hospital.gestion.service;

import com.hospital.gestion.model.Departamento;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Incidencia;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.repository.DepartamentoRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.IncidenciaRepository;
import com.hospital.gestion.repository.TurnoRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InformeServiceTest {

    private final TurnoRepository turnos = mock(TurnoRepository.class);
    private final EmpleadoRepository empleados = mock(EmpleadoRepository.class);
    private final DepartamentoRepository departamentos = mock(DepartamentoRepository.class);
    private final IncidenciaRepository incidencias = mock(IncidenciaRepository.class);
    private final InformeService servicio = new InformeService(turnos, empleados, departamentos, incidencias);

    private final Departamento pediatria = departamento();
    private final Empleado ana = empleado(pediatria);

    // ---------- ayudas ----------

    private static Departamento departamento() {
        Departamento d = new Departamento();
        d.setIdDepartamento(1);
        d.setNombreDept("Pediatría");
        d.setCapCobertura(4);
        return d;
    }

    private static Empleado empleado(Departamento d) {
        Empleado e = new Empleado();
        e.setIdEmpleado(1);
        e.setNombre("Ana");
        e.setApellido("López");
        e.setDepartamento(d);
        return e;
    }

    private Turno turno(LocalDate fecha, String inicio, String fin, String estado) {
        Turno t = new Turno();
        t.setEmpleado(ana);
        t.setDepartamento(pediatria);
        t.setFecha(fecha);
        t.setHoraInicio(LocalTime.parse(inicio));
        t.setHoraFin(LocalTime.parse(fin));
        t.setEstadoTurno(estado);
        return t;
    }

    private Incidencia incidencia(String tipo) {
        Incidencia i = new Incidencia();
        i.setEmpleado(ana);
        i.setTipoIncidencia(tipo);
        return i;
    }

    private static double numero(Object valor) {
        return ((Number) valor).doubleValue();
    }

    // ---------- horas extras ----------

    // ---------- horas diurnas, nocturnas y extras ----------

    private Informe horasExtras(Turno... ts) {
        when(empleados.findAll()).thenReturn(List.of(ana));
        when(turnos.findByFechaBetween(any(), any())).thenReturn(Arrays.asList(ts));
        return servicio.generar("horas-extras", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 8);
    }

    /** Columnas: 3 trabajadas, 4 diurnas, 5 nocturnas, 6 ordinarias, 7 extras, 8 extras diurnas, 9 extras nocturnas, 10 días con extras. */
    private static void assertHoras(List<Object> fila, double total, double diur, double noct, double ord,
                                    double extras, double extDiur, double extNoct) {
        assertEquals(total, numero(fila.get(3)), 0.001, "trabajadas");
        assertEquals(diur, numero(fila.get(4)), 0.001, "diurnas");
        assertEquals(noct, numero(fila.get(5)), 0.001, "nocturnas");
        assertEquals(ord, numero(fila.get(6)), 0.001, "ordinarias");
        assertEquals(extras, numero(fila.get(7)), 0.001, "extras");
        assertEquals(extDiur, numero(fila.get(8)), 0.001, "extras diurnas");
        assertEquals(extNoct, numero(fila.get(9)), 0.001, "extras nocturnas");
    }

    private static final LocalDate DIA = LocalDate.of(2026, 11, 3);

    @Test
    void horasExtrasSonLoQueSePasaDeLaJornadaDiaria() {
        Informe informe = horasExtras(turno(DIA, "07:00", "19:00", "Cumplido"));

        assertEquals(1, informe.filas().size());
        List<Object> fila = informe.filas().get(0);
        assertHoras(fila, 12, 12, 0, 8, 4, 4, 0);
        assertEquals(1, (int) numero(fila.get(10)));   // días con extras
    }

    @Test
    void losTurnosDelMismoDiaSeSumanYLasExtrasSonLasUltimasHoras() {
        // 06-12 (6 h) y 14-20 (6 h): las primeras 8 h son ordinarias; las extras son 16:00-20:00 (3 h de día y 1 h de noche)
        Informe informe = horasExtras(turno(DIA, "06:00", "12:00", "Cumplido"), turno(DIA, "14:00", "20:00", "Cumplido"));

        assertHoras(informe.filas().get(0), 12, 11, 1, 8, 4, 3, 1);
    }

    @Test
    void unTurnoNocturnoCuentaComoHorasNocturnas() {
        Informe informe = horasExtras(turno(DIA, "22:00", "06:00", "Cumplido"));

        assertHoras(informe.filas().get(0), 8, 0, 8, 8, 0, 0, 0);
    }

    @Test
    void unTurnoQueCruzaElLimiteSeRepartiEntreDiaYNoche() {
        Informe informe = horasExtras(turno(DIA, "18:00", "02:00", "Cumplido"));

        assertHoras(informe.filas().get(0), 8, 1, 7, 8, 0, 0, 0);   // 18-19 de día, 19-02 de noche
    }

    @Test
    void lasHorasExtrasTambienSeClasificanComoNocturnas() {
        // 14:00-23:00 (9 h): ordinarias 14-22 (5 de día y 3 de noche); extra 22-23 (de noche)
        Informe informe = horasExtras(turno(DIA, "14:00", "23:00", "Cumplido"));

        assertHoras(informe.filas().get(0), 9, 5, 4, 8, 1, 0, 1);
    }

    @Test
    void elHorarioNocturnoSePuedeConfigurar() {
        when(empleados.findAll()).thenReturn(List.of(ana));
        when(turnos.findByFechaBetween(any(), any())).thenReturn(List.of(turno(DIA, "18:00", "23:00", "Cumplido")));

        Informe informe = servicio.generar("horas-extras", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 8,
                LocalTime.of(20, 0), LocalTime.of(5, 0));

        assertHoras(informe.filas().get(0), 5, 2, 3, 5, 0, 0, 0);   // noche desde las 20:00
    }

    @Test
    void elHorarioNocturnoDebeTenerInicioYFinDistintos() {
        assertThrows(IllegalArgumentException.class, () -> servicio.generar("horas-extras",
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 8, LocalTime.of(19, 0), LocalTime.of(19, 0)));
    }

    @Test
    void soloCuentanLosTurnosCumplidos() {
        LocalDate dia = LocalDate.of(2026, 11, 3);
        when(empleados.findAll()).thenReturn(List.of(ana));
        when(turnos.findByFechaBetween(any(), any())).thenReturn(List.of(turno(dia, "07:00", "19:00", "Programado")));

        Informe informe = servicio.generar("horas-extras", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 8);

        assertTrue(informe.filas().isEmpty());
    }

    // ---------- asistencia ----------

    @Test
    void elCumplimientoSoloMideLosTurnosQueYaOcurrieron() {
        LocalDate hoy = LocalDate.now();
        when(empleados.findAll()).thenReturn(List.of(ana));
        when(turnos.findByFechaBetween(any(), any())).thenReturn(List.of(
                turno(hoy.minusDays(5), "07:00", "15:00", "Cumplido"),
                turno(hoy.minusDays(3), "07:00", "15:00", "Programado"),
                turno(hoy.plusDays(3), "07:00", "15:00", "Programado"),
                turno(hoy.minusDays(2), "07:00", "15:00", "Cancelado")));
        when(incidencias.entre(any(), any())).thenReturn(List.of(incidencia("Retraso"), incidencia("Retraso"), incidencia("Inasistencia")));

        Informe informe = servicio.generar("asistencia", hoy.minusDays(10), hoy.plusDays(10), null, 8);

        List<Object> fila = informe.filas().get(0);
        assertEquals(3, (int) numero(fila.get(2)));         // asignados (sin cancelados)
        assertEquals(1, (int) numero(fila.get(3)));         // cumplidos
        assertEquals(1, (int) numero(fila.get(4)));         // cancelados
        assertEquals(1, (int) numero(fila.get(5)));         // inasistencias
        assertEquals(2, (int) numero(fila.get(6)));         // retrasos
        assertEquals(50.0, numero(fila.get(8)), 0.001);     // 1 cumplido de 2 que ya ocurrieron
    }

    // ---------- cobertura ----------

    @Test
    void laCoberturaComparaElPersonalDelDiaContraLaCapacidad() {
        LocalDate lunes = LocalDate.of(2026, 11, 9);
        when(departamentos.findAll()).thenReturn(List.of(pediatria));
        when(turnos.findByFechaBetween(any(), any())).thenReturn(List.of(
                turno(lunes, "07:00", "15:00", "Programado"),
                turno(lunes.plusDays(1), "07:00", "15:00", "Programado"),
                turno(lunes.plusDays(2), "07:00", "15:00", "Programado"),
                turno(lunes.plusDays(3), "07:00", "15:00", "Programado")));

        Informe informe = servicio.generar("cobertura", lunes, lunes.plusDays(3), null, 8);

        List<Object> fila = informe.filas().get(0);
        assertEquals(4, (int) numero(fila.get(1)));          // capacidad
        assertEquals(4, (int) numero(fila.get(2)));          // días del periodo
        assertEquals(1.0, numero(fila.get(3)), 0.001);       // personal promedio por día
        assertEquals(25.0, numero(fila.get(4)), 0.001);      // cobertura promedio (1 de 4)
        assertEquals(0, (int) numero(fila.get(5)));          // días completos
        assertEquals(4, (int) numero(fila.get(6)));          // días con cobertura baja
    }

    // ---------- validaciones ----------

    @Test
    void laFechaHastaNoPuedeSerAnteriorADesde() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.generar("asistencia", LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1), null, 8));
    }

    @Test
    void unTipoDeReporteDesconocidoSeRechaza() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.generar("nomina", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 8));
    }

    @Test
    void laJornadaDebeEstarEntreCeroYVeinticuatroHoras() {
        assertThrows(IllegalArgumentException.class, () ->
                servicio.generar("horas-extras", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, 30));
    }
}
