package com.hospital.gestion.service;

import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Departamento;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.model.Incidencia;
import com.hospital.gestion.model.Turno;
import com.hospital.gestion.repository.DepartamentoRepository;
import com.hospital.gestion.repository.EmpleadoRepository;
import com.hospital.gestion.repository.IncidenciaRepository;
import com.hospital.gestion.repository.TurnoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class InformeService {

    public static final String ASISTENCIA = "asistencia", COBERTURA = "cobertura", HORAS_EXTRAS = "horas-extras";

    /** Horario nocturno por defecto: de 19:00 a 06:00. */
    private static final LocalTime NOCHE_INICIO = LocalTime.of(19, 0), NOCHE_FIN = LocalTime.of(6, 0);

    private final TurnoRepository turnos;
    private final EmpleadoRepository empleados;
    private final DepartamentoRepository departamentos;
    private final IncidenciaRepository incidencias;

    public InformeService(TurnoRepository turnos, EmpleadoRepository empleados,
                          DepartamentoRepository departamentos, IncidenciaRepository incidencias) {
        this.turnos = turnos;
        this.empleados = empleados;
        this.departamentos = departamentos;
        this.incidencias = incidencias;
    }

    /** Valor que se guarda en Reporte.tipo_reporte (respeta el CHECK de la base). */
    public static String tipoReporte(String slug) {
        return switch (slug) {
            case ASISTENCIA -> "Asistencia";
            case COBERTURA -> "Cobertura";
            case HORAS_EXTRAS -> "Horas Extras";
            default -> throw new IllegalArgumentException(
                    "Tipo de reporte desconocido: " + slug + " (use asistencia, cobertura u horas-extras)");
        };
    }

    public Informe generar(String slug, LocalDate desdeParam, LocalDate hastaParam, Integer dept, double jornada) {
        return generar(slug, desdeParam, hastaParam, dept, jornada, null, null);
    }

    /** nocheDesde y nocheHasta: horario nocturno para el reporte de horas extras (por defecto 19:00 a 06:00). */
    public Informe generar(String slug, LocalDate desdeParam, LocalDate hastaParam, Integer dept, double jornada,
                           LocalTime nocheDesde, LocalTime nocheHasta) {
        String tipo = tipoReporte(slug);
        final LocalTime nocheInicio = nocheDesde != null ? nocheDesde : NOCHE_INICIO;
        final LocalTime nocheFin = nocheHasta != null ? nocheHasta : NOCHE_FIN;
        if (nocheInicio.equals(nocheFin)) {
            throw new IllegalArgumentException("El horario nocturno debe tener una hora de inicio distinta de la hora de fin");
        }
        final LocalDate desde = desdeParam != null ? desdeParam : LocalDate.now().withDayOfMonth(1);
        final LocalDate hasta = hastaParam != null ? hastaParam : desde.withDayOfMonth(desde.lengthOfMonth());
        if (hasta.isBefore(desde)) throw new IllegalArgumentException("La fecha 'hasta' no puede ser anterior a 'desde'");
        if (ChronoUnit.DAYS.between(desde, hasta) > 366) throw new IllegalArgumentException("El periodo no puede superar 366 días");
        if (jornada <= 0 || jornada > 24) throw new IllegalArgumentException("La jornada debe estar entre 0 y 24 horas");

        String periodo = "Del " + desde + " al " + hasta + " · "
                + (dept == null ? "Todos los departamentos" : nombreDept(dept));
        if (HORAS_EXTRAS.equals(slug)) {
            periodo += " · Jornada ordinaria: " + jornada + " h/día · Horario nocturno: " + nocheInicio + " a " + nocheFin;
        }

        return switch (slug) {
            case ASISTENCIA -> asistencia(tipo, periodo, desde, hasta, dept);
            case COBERTURA -> cobertura(tipo, periodo, desde, hasta, dept);
            default -> horasExtras(tipo, periodo, desde, hasta, dept, jornada, nocheInicio, nocheFin);
        };
    }

    // ---------------- ASISTENCIA ----------------

    private Informe asistencia(String tipo, String periodo, LocalDate desde, LocalDate hasta, Integer dept) {
        LocalDate hoy = LocalDate.now();
        Map<Integer, List<Turno>> turnosPorEmp = turnos.findByFechaBetween(desde, hasta).stream()
                .collect(Collectors.groupingBy(t -> t.getEmpleado().getIdEmpleado()));
        Map<Integer, List<Incidencia>> incPorEmp = incidencias
                .entre(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay()).stream()
                .collect(Collectors.groupingBy(i -> i.getEmpleado().getIdEmpleado()));

        List<List<Object>> filas = new ArrayList<>();
        int tAsig = 0, tCump = 0, tCanc = 0, tInas = 0, tRet = 0, tInc = 0, tVenc = 0, tCumpVenc = 0;
        for (Empleado e : empleadosDe(dept)) {
            List<Turno> todos = turnosPorEmp.getOrDefault(e.getIdEmpleado(), List.of());
            List<Turno> activos = todos.stream().filter(t -> !"Cancelado".equals(t.getEstadoTurno())).toList();
            List<Turno> vencidos = activos.stream().filter(t -> !t.getFecha().isAfter(hoy)).toList();
            int cumplidos = (int) activos.stream().filter(t -> "Cumplido".equals(t.getEstadoTurno())).count();
            int cumpVenc = (int) vencidos.stream().filter(t -> "Cumplido".equals(t.getEstadoTurno())).count();
            int cancelados = todos.size() - activos.size();
            List<Incidencia> incs = incPorEmp.getOrDefault(e.getIdEmpleado(), List.of());
            int inas = contar(incs, "Inasistencia"), ret = contar(incs, "Retraso"), inc = contar(incs, "Incapacidad");

            filas.add(fila(nombre(e), e.getDepartamento().getNombreDept(), activos.size(), cumplidos, cancelados,
                    inas, ret, inc, vencidos.isEmpty() ? null : redondear(cumpVenc * 100.0 / vencidos.size(), 1)));
            tAsig += activos.size(); tCump += cumplidos; tCanc += cancelados;
            tInas += inas; tRet += ret; tInc += inc; tVenc += vencidos.size(); tCumpVenc += cumpVenc;
        }
        List<Object> totales = fila("TOTAL", "", tAsig, tCump, tCanc, tInas, tRet, tInc,
                tVenc == 0 ? null : redondear(tCumpVenc * 100.0 / tVenc, 1));
        return new Informe(tipo, "Reporte de asistencia", periodo,
                List.of("Empleado", "Departamento", "Turnos asignados", "Cumplidos", "Cancelados",
                        "Inasistencias", "Retrasos", "Incapacidades", "Cumplimiento (%)"),
                filas, totales,
                "Turnos asignados = turnos no cancelados. Cumplimiento = turnos cumplidos / turnos no cancelados que ya ocurrieron (fecha hasta hoy). "
                + "Las inasistencias, retrasos e incapacidades salen de las incidencias reportadas en el periodo.");
    }

    // ---------------- COBERTURA ----------------

    private Informe cobertura(String tipo, String periodo, LocalDate desde, LocalDate hasta, Integer dept) {
        List<Departamento> deps = dept == null ? departamentos.findAll()
                : List.of(departamentos.findById(dept).orElseThrow(() -> new ResourceNotFoundException("Departamento no encontrado")));
        List<Turno> activos = turnos.findByFechaBetween(desde, hasta).stream()
                .filter(t -> !"Cancelado".equals(t.getEstadoTurno())).toList();
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;

        List<List<Object>> filas = new ArrayList<>();
        for (Departamento d : deps) {
            Map<LocalDate, Set<Integer>> porDia = activos.stream()
                    .filter(t -> t.getDepartamento().getIdDepartamento().equals(d.getIdDepartamento()))
                    .collect(Collectors.groupingBy(Turno::getFecha,
                            Collectors.mapping(t -> t.getEmpleado().getIdEmpleado(), Collectors.toSet())));
            int cap = d.getCapCobertura();
            double sumPersonal = 0, sumPct = 0;
            int completos = 0, bajos = 0;
            for (LocalDate x = desde; !x.isAfter(hasta); x = x.plusDays(1)) {
                int n = porDia.containsKey(x) ? porDia.get(x).size() : 0;
                double pct = cap > 0 ? Math.min(100.0, n * 100.0 / cap) : 100.0;
                sumPersonal += n;
                sumPct += pct;
                if (n >= cap) completos++;
                if (pct < 50) bajos++;
            }
            filas.add(fila(d.getNombreDept(), cap, (int) dias, redondear(sumPersonal / dias, 1),
                    redondear(sumPct / dias, 1), completos, bajos));
        }
        filas.sort((a, b) -> Double.compare((Double) a.get(4), (Double) b.get(4)));
        return new Informe(tipo, "Reporte de cobertura por departamento", periodo,
                List.of("Departamento", "Capacidad", "Días", "Personal promedio/día", "Cobertura promedio (%)",
                        "Días con cobertura completa", "Días con cobertura baja"),
                filas, null,
                "Personal por día = empleados distintos con turno no cancelado en el departamento. Cobertura = personal / capacidad "
                + "(máximo 100 % por día). Cobertura baja = menos del 50 %. Se cuentan todos los días del periodo.");
    }

    // ---------------- HORAS EXTRAS ----------------

    private Informe horasExtras(String tipo, String periodo, LocalDate desde, LocalDate hasta, Integer dept, double jornada,
                                LocalTime nocheInicio, LocalTime nocheFin) {
        Map<Integer, List<Turno>> porEmp = turnos.findByFechaBetween(desde, hasta).stream()
                .filter(t -> "Cumplido".equals(t.getEstadoTurno()))
                .collect(Collectors.groupingBy(t -> t.getEmpleado().getIdEmpleado()));
        final int cuotaDiaria = (int) Math.round(jornada * 60);   // minutos ordinarios de cada día

        List<List<Object>> filas = new ArrayList<>();
        int tTurnos = 0, tDias = 0;
        long tDiur = 0, tNoct = 0, tExtDiur = 0, tExtNoct = 0;
        for (Empleado e : empleadosDe(dept)) {
            List<Turno> ts = porEmp.get(e.getIdEmpleado());
            if (ts == null || ts.isEmpty()) continue;
            Map<LocalDate, List<Turno>> porDia = ts.stream().collect(Collectors.groupingBy(Turno::getFecha));
            long diur = 0, noct = 0, extDiur = 0, extNoct = 0;
            int diasExtra = 0;
            for (List<Turno> delDia : porDia.values()) {
                // Dentro de cada día, las primeras horas (según la jornada) son ordinarias y el resto son extras
                List<Turno> ordenados = new ArrayList<>(delDia);
                ordenados.sort(Comparator.comparing(Turno::getHoraInicio));
                int cuota = cuotaDiaria;
                boolean huboExtras = false;
                for (Turno t : ordenados) {
                    int inicio = t.getHoraInicio().toSecondOfDay() / 60;
                    int duracion = minutos(t);
                    int ordinarios = Math.min(cuota, duracion);
                    cuota -= ordinarios;
                    long[] parteOrdinaria = repartir(inicio, inicio + ordinarios, nocheInicio, nocheFin);
                    long[] parteExtra = repartir(inicio + ordinarios, inicio + duracion, nocheInicio, nocheFin);
                    diur += parteOrdinaria[0] + parteExtra[0];
                    noct += parteOrdinaria[1] + parteExtra[1];
                    extDiur += parteExtra[0];
                    extNoct += parteExtra[1];
                    if (duracion > ordinarios) huboExtras = true;
                }
                if (huboExtras) diasExtra++;
            }
            long total = diur + noct, extras = extDiur + extNoct;
            filas.add(fila(nombre(e), e.getDepartamento().getNombreDept(), ts.size(), aHoras(total), aHoras(diur), aHoras(noct),
                    aHoras(total - extras), aHoras(extras), aHoras(extDiur), aHoras(extNoct), diasExtra));
            tTurnos += ts.size(); tDias += diasExtra;
            tDiur += diur; tNoct += noct; tExtDiur += extDiur; tExtNoct += extNoct;
        }
        filas.sort((a, b) -> Double.compare((Double) b.get(7), (Double) a.get(7)));
        long tTotal = tDiur + tNoct, tExtras = tExtDiur + tExtNoct;
        return new Informe(tipo, "Reporte de horas extras", periodo,
                List.of("Empleado", "Departamento", "Turnos cumplidos", "Horas trabajadas", "Horas diurnas", "Horas nocturnas",
                        "Horas ordinarias", "Horas extras", "Extras diurnas", "Extras nocturnas", "Días con extras"),
                filas, fila("TOTAL", "", tTurnos, aHoras(tTotal), aHoras(tDiur), aHoras(tNoct), aHoras(tTotal - tExtras),
                        aHoras(tExtras), aHoras(tExtDiur), aHoras(tExtNoct), tDias),
                "Solo cuentan los turnos en estado Cumplido. En cada día, los turnos se ordenan por hora de inicio: las primeras horas "
                + "(según la jornada ordinaria) son ordinarias y lo que pase es hora extra. Horas nocturnas = las que caen entre "
                + nocheInicio + " y " + nocheFin + "; el resto son diurnas. Un turno que cruza la medianoche se cuenta en el día en que empieza.");
    }

    /** Reparte los minutos de [desde, hasta) (contados desde las 00:00 del día de inicio) entre diurnos y nocturnos. */
    private static long[] repartir(int desde, int hasta, LocalTime nocheInicio, LocalTime nocheFin) {
        int ni = nocheInicio.toSecondOfDay() / 60, nf = nocheFin.toSecondOfDay() / 60;
        long dia = 0, noche = 0;
        for (int m = desde; m < hasta; m++) {
            int t = m % 1440;
            boolean esNoche = ni < nf ? (t >= ni && t < nf) : (t >= ni || t < nf);
            if (esNoche) noche++; else dia++;
        }
        return new long[]{dia, noche};
    }

    private static int minutos(Turno t) {
        long min = Duration.between(t.getHoraInicio(), t.getHoraFin()).toMinutes();
        if (min <= 0) min += 24 * 60;   // el turno cruza la medianoche
        return (int) min;
    }

    private static double aHoras(long minutos) {
        return redondear(minutos / 60.0, 2);
    }

    // ---------------- Utilidades ----------------

    private double horas(Turno t) {
        long min = Duration.between(t.getHoraInicio(), t.getHoraFin()).toMinutes();
        if (min <= 0) min += 24 * 60;   // turno que cruza la medianoche
        return min / 60.0;
    }

    private List<Empleado> empleadosDe(Integer dept) {
        return empleados.findAll().stream()
                .filter(e -> dept == null || e.getDepartamento().getIdDepartamento().equals(dept))
                .sorted(Comparator.comparing((Empleado e) -> e.getDepartamento().getNombreDept())
                        .thenComparing(Empleado::getApellido).thenComparing(Empleado::getNombre))
                .toList();
    }

    private String nombreDept(Integer id) {
        return departamentos.findById(id).map(Departamento::getNombreDept)
                .orElseThrow(() -> new ResourceNotFoundException("Departamento con id " + id + " no encontrado"));
    }

    private static String nombre(Empleado e) {
        return e.getNombre() + " " + e.getApellido();
    }

    private static int contar(List<Incidencia> lista, String tipo) {
        return (int) lista.stream().filter(i -> tipo.equals(i.getTipoIncidencia())).count();
    }

    private static List<Object> fila(Object... valores) {
        return new ArrayList<>(Arrays.asList(valores));
    }

    private static double redondear(double v, int decimales) {
        double f = Math.pow(10, decimales);
        return Math.round(v * f) / f;
    }
}
