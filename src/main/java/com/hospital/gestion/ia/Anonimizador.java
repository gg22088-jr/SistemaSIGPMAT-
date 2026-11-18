package com.hospital.gestion.ia;

import com.hospital.gestion.service.Informe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Antes de enviar un reporte a un servicio externo se cambian los nombres de las personas por códigos (E01, E02...).
 * Cuando llega el análisis, los códigos se vuelven a cambiar por los nombres, solo para quien lo pidió.
 */
public final class Anonimizador {

    private static final int MAX_FILAS = 200;
    private static final Pattern CODIGO = Pattern.compile("\\bE(\\d{2,3})\\b");

    private Anonimizador() { }

    /** Devuelve una copia del reporte con la columna Empleado anonimizada; los nombres originales quedan en la lista. */
    public static Informe anonimizar(Informe informe, List<String> nombres) {
        int columna = informe.columnas().indexOf("Empleado");
        if (columna < 0) return informe;
        List<List<Object>> filas = new ArrayList<>();
        for (List<Object> fila : informe.filas()) {
            List<Object> copia = new ArrayList<>(fila);
            nombres.add(String.valueOf(fila.get(columna)));
            copia.set(columna, String.format("E%02d", nombres.size()));
            filas.add(copia);
        }
        return new Informe(informe.tipo(), informe.titulo(), informe.periodo(), informe.columnas(), filas,
                informe.totales(), informe.nota());
    }

    /** Cambia los códigos E01, E02... del texto por los nombres que representan. */
    public static String restaurar(String texto, List<String> nombres) {
        Matcher m = CODIGO.matcher(texto);
        StringBuilder salida = new StringBuilder();
        while (m.find()) {
            int posicion = Integer.parseInt(m.group(1)) - 1;
            String reemplazo = (posicion >= 0 && posicion < nombres.size()) ? nombres.get(posicion) : m.group();
            m.appendReplacement(salida, Matcher.quoteReplacement(reemplazo));
        }
        m.appendTail(salida);
        return salida.toString();
    }

    /** El reporte como texto simple (tabla con barras verticales) para enviarlo al modelo. */
    public static String aTexto(Informe informe) {
        StringBuilder sb = new StringBuilder();
        sb.append(informe.titulo()).append('\n').append(informe.periodo()).append("\n\n");
        sb.append(String.join(" | ", informe.columnas())).append('\n');
        int limite = Math.min(informe.filas().size(), MAX_FILAS);
        for (int i = 0; i < limite; i++) sb.append(linea(informe.filas().get(i))).append('\n');
        if (informe.filas().size() > limite) sb.append("(se omiten ").append(informe.filas().size() - limite).append(" filas)\n");
        if (informe.totales() != null) sb.append("TOTAL: ").append(linea(informe.totales())).append('\n');
        if (informe.nota() != null) sb.append("\nNota del reporte: ").append(informe.nota()).append('\n');
        return sb.toString();
    }

    private static String linea(List<Object> fila) {
        return fila.stream().map(v -> v == null ? "—" : String.valueOf(v)).collect(Collectors.joining(" | "));
    }
}
