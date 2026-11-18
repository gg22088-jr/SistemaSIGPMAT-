package com.hospital.gestion.ia;

import com.hospital.gestion.service.Informe;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Análisis básico por reglas, sin IA ni red: se usa cuando no hay clave o si el servicio de IA falla. */
public final class AnalisisLocal {

    private static final double UMBRAL_CUMPLIMIENTO = 80;
    private static final double UMBRAL_COBERTURA = 50;
    private static final double UMBRAL_EXTRAS = 20;

    private AnalisisLocal() { }

    public static String analizar(Informe informe) {
        if (informe.filas().isEmpty()) return "No hay datos en el periodo seleccionado para analizar.";
        return switch (informe.tipo()) {
            case "Asistencia" -> asistencia(informe);
            case "Cobertura" -> cobertura(informe);
            case "Horas Extras" -> horasExtras(informe);
            default -> "No hay un análisis disponible para este tipo de reporte.";
        };
    }

    // Columnas: 0 Empleado, 1 Departamento, 2 asignados, 3 cumplidos, 4 cancelados, 5 inasistencias, 6 retrasos, 7 incapacidades, 8 cumplimiento %
    private static String asistencia(Informe inf) {
        List<Object> t = inf.totales();
        StringBuilder sb = new StringBuilder("Resumen de asistencia\n\n");
        sb.append("• Turnos asignados: ").append(ent(t.get(2))).append("; cumplidos: ").append(ent(t.get(3)))
          .append("; cancelados: ").append(ent(t.get(4))).append(".\n");
        if (t.get(8) == null) {
            sb.append("• Todavía no hay turnos que ya hayan ocurrido para medir el cumplimiento.\n");
        } else {
            sb.append("• Cumplimiento general: ").append(n1(num(t.get(8)))).append(" % de los turnos que ya ocurrieron.\n");
        }
        sb.append("• Incidencias: ").append(ent(t.get(5))).append(" inasistencias, ").append(ent(t.get(6)))
          .append(" retrasos y ").append(ent(t.get(7))).append(" incapacidades.\n");

        List<List<Object>> bajos = inf.filas().stream()
                .filter(f -> f.get(8) != null && num(f.get(8)) < UMBRAL_CUMPLIMIENTO)
                .sorted(Comparator.comparingDouble((List<Object> f) -> num(f.get(8))))
                .limit(3).toList();
        if (!bajos.isEmpty()) {
            sb.append("\nAtención: cumplimiento menor a ").append((int) UMBRAL_CUMPLIMIENTO).append(" % en ")
              .append(bajos.stream().map(f -> f.get(0) + " (" + n1(num(f.get(8))) + " %)").collect(Collectors.joining(", ")))
              .append(".\n");
        }
        sb.append("\nRecomendación: ");
        if (!bajos.isEmpty() || num(t.get(5)) > 0) {
            sb.append("revisar los casos con menor cumplimiento y las inasistencias registradas, y confirmar que los turnos cumplidos estén marcados como tales.");
        } else {
            sb.append("la asistencia del periodo es adecuada; mantener el seguimiento.");
        }
        return sb.toString();
    }

    // Columnas: 0 Departamento, 1 Capacidad, 2 Días, 3 Personal promedio, 4 Cobertura %, 5 Días completos, 6 Días con cobertura baja
    private static String cobertura(Informe inf) {
        List<List<Object>> filas = inf.filas();
        double promedio = filas.stream().mapToDouble(f -> num(f.get(4))).average().orElse(0);
        List<List<Object>> bajas = filas.stream().filter(f -> num(f.get(4)) < UMBRAL_COBERTURA).toList();
        StringBuilder sb = new StringBuilder("Resumen de cobertura\n\n");
        sb.append("• Cobertura promedio de los departamentos analizados: ").append(n1(promedio)).append(" %.\n");
        sb.append("• Departamentos con cobertura promedio menor a ").append((int) UMBRAL_COBERTURA).append(" %: ")
          .append(bajas.size()).append(" de ").append(filas.size()).append(".\n");
        List<List<Object>> peores = filas.stream()
                .sorted(Comparator.comparingDouble((List<Object> f) -> num(f.get(4)))).limit(3).toList();
        sb.append("• Menor cobertura: ")
          .append(peores.stream().map(f -> f.get(0) + " (" + n1(num(f.get(4))) + " %, " + ent(f.get(6)) + " de " + ent(f.get(2)) + " días con cobertura baja)")
                  .collect(Collectors.joining("; ")))
          .append(".\n");
        List<String> sinProblemas = filas.stream().filter(f -> num(f.get(6)) == 0).map(f -> String.valueOf(f.get(0))).toList();
        if (!sinProblemas.isEmpty()) {
            sb.append("• Sin días de cobertura baja: ").append(String.join(", ", sinProblemas)).append(".\n");
        }
        sb.append("\nRecomendación: ");
        if (bajas.size() == filas.size() && filas.size() > 1) {
            sb.append("casi todos los departamentos salen con cobertura baja; conviene revisar si la capacidad de cobertura configurada en cada departamento es realista.");
        } else if (!bajas.isEmpty()) {
            sb.append("reforzar la asignación de turnos en los departamentos con menor cobertura.");
        } else {
            sb.append("la cobertura del periodo es adecuada; mantener el seguimiento.");
        }
        return sb.toString();
    }

    // Columnas: 0 Empleado, 1 Departamento, 2 turnos, 3 trabajadas, 4 diurnas, 5 nocturnas, 6 ordinarias, 7 extras, 8 extras diurnas, 9 extras nocturnas, 10 días con extras
    private static String horasExtras(Informe inf) {
        List<Object> t = inf.totales();
        double trabajadas = num(t.get(3)), nocturnas = num(t.get(5)), extras = num(t.get(7)), extrasNocturnas = num(t.get(9));
        StringBuilder sb = new StringBuilder("Resumen de horas extras\n\n");
        sb.append("• Horas trabajadas en turnos cumplidos: ").append(n1(trabajadas)).append(" (").append(n1(nocturnas)).append(" nocturnas).\n");
        if (extras == 0) {
            sb.append("• No hay horas extras en el periodo.\n\nRecomendación: sin acciones; las jornadas se mantuvieron dentro del límite diario.");
            return sb.toString();
        }
        sb.append("• Horas extras: ").append(n1(extras)).append(" (").append(n1(extrasNocturnas)).append(" nocturnas y ")
          .append(n1(num(t.get(8)))).append(" diurnas).\n");
        List<List<Object>> top = inf.filas().stream().filter(f -> num(f.get(7)) > 0)
                .sorted(Comparator.comparingDouble((List<Object> f) -> num(f.get(7))).reversed()).limit(3).toList();
        sb.append("• Mayor cantidad de horas extras: ")
          .append(top.stream().map(f -> f.get(0) + " (" + n1(num(f.get(7))) + " h en " + ent(f.get(10)) + " días)").collect(Collectors.joining("; ")))
          .append(".\n");
        List<String> altas = inf.filas().stream().filter(f -> num(f.get(7)) > UMBRAL_EXTRAS).map(f -> String.valueOf(f.get(0))).toList();
        if (!altas.isEmpty()) {
            sb.append("\nAtención: más de ").append((int) UMBRAL_EXTRAS).append(" horas extras en el periodo: ").append(String.join(", ", altas)).append(".\n");
        }
        sb.append("\nRecomendación: ");
        sb.append(altas.isEmpty()
                ? "revisar si las horas extras se concentran en pocas personas o días y repartir la carga cuando sea posible."
                : "revisar la carga de trabajo de las personas señaladas y considerar redistribuir turnos.");
        return sb.toString();
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }

    private static String n1(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    private static String ent(Object o) {
        return String.valueOf(Math.round(num(o)));
    }
}
