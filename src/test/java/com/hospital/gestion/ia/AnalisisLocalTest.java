package com.hospital.gestion.ia;

import com.hospital.gestion.service.Informe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalisisLocalTest {

    private static List<Object> fila(Object... valores) {
        return new ArrayList<>(Arrays.asList(valores));
    }

    private static Informe informe(String tipo, List<List<Object>> filas, List<Object> totales) {
        return new Informe(tipo, "Reporte", "Del 2026-11-01 al 2026-11-30", List.of("a"), filas, totales, null);
    }

    @Test
    void sinDatosSeAvisa() {
        String texto = AnalisisLocal.analizar(informe("Asistencia", List.of(), null));
        assertTrue(texto.contains("No hay datos"));
    }

    @Test
    void laAsistenciaSenalaAQuienTieneBajoCumplimiento() {
        Informe inf = informe("Asistencia",
                List.of(fila("Ana López", "Pediatría", 3, 2, 0, 1, 1, 0, 66.7), fila("Luis Torres", "Cardiología", 4, 4, 0, 0, 0, 0, 100.0)),
                fila("TOTAL", "", 7, 6, 0, 1, 1, 0, 85.7));

        String texto = AnalisisLocal.analizar(inf);

        assertTrue(texto.contains("85.7"), texto);
        assertTrue(texto.contains("Ana López (66.7 %)"), texto);
        assertTrue(!texto.contains("Luis Torres"), "quien cumple al 100 % no debe señalarse");
        assertTrue(texto.contains("1 inasistencias"), texto);
    }

    @Test
    void laCoberturaSenalaLosDepartamentosConMenorCobertura() {
        Informe inf = informe("Cobertura",
                List.of(fila("Emergencias", 30, 30, 1.0, 3.3, 0, 30), fila("Pediatría", 4, 30, 4.0, 100.0, 30, 0)),
                null);

        String texto = AnalisisLocal.analizar(inf);

        assertTrue(texto.contains("Emergencias (3.3 %"), texto);
        assertTrue(texto.contains("Sin días de cobertura baja: Pediatría"), texto);
        assertTrue(texto.contains("1 de 2"), texto);
    }

    @Test
    void lasHorasExtrasSenalanAQuienSuperaElUmbral() {
        Informe inf = informe("Horas Extras",
                List.of(fila("Ana López", "Pediatría", 10, 120.0, 90.0, 30.0, 96.0, 24.0, 20.0, 4.0, 6),
                        fila("Luis Torres", "Cardiología", 5, 40.0, 40.0, 0.0, 38.0, 2.0, 2.0, 0.0, 1)),
                fila("TOTAL", "", 15, 160.0, 130.0, 30.0, 134.0, 26.0, 22.0, 4.0, 7));

        String texto = AnalisisLocal.analizar(inf);

        assertTrue(texto.contains("26.0"), texto);
        assertTrue(texto.contains("Ana López (24.0 h en 6 días)"), texto);
        assertTrue(texto.contains("Atención: más de 20 horas extras"), texto);
    }

    @Test
    void sinHorasExtrasSeDiceAsi() {
        Informe inf = informe("Horas Extras",
                List.of(fila("Ana López", "Pediatría", 2, 16.0, 16.0, 0.0, 16.0, 0.0, 0.0, 0.0, 0)),
                fila("TOTAL", "", 2, 16.0, 16.0, 0.0, 16.0, 0.0, 0.0, 0.0, 0));

        assertTrue(AnalisisLocal.analizar(inf).contains("No hay horas extras"));
    }
}
