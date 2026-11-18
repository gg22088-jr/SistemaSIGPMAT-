package com.hospital.gestion.ia;

import com.hospital.gestion.service.Informe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnonimizadorTest {

    private static Informe informe(List<String> columnas, List<Object>... filas) {
        List<List<Object>> lista = new ArrayList<>();
        for (List<Object> f : filas) lista.add(new ArrayList<>(f));
        return new Informe("Horas Extras", "Reporte de horas extras", "Del 2026-11-01 al 2026-11-30", columnas, lista,
                Arrays.asList("TOTAL", "", 3), "nota");
    }

    @SafeVarargs
    private static List<Object> fila(Object... valores) {
        return Arrays.asList(valores);
    }

    @Test
    void losNombresSeCambianPorCodigosYElOriginalNoSeModifica() {
        Informe original = informe(List.of("Empleado", "Departamento", "Turnos"),
                fila("Ana López", "Pediatría", 2), fila("Luis Torres", "Cardiología", 1));
        List<String> nombres = new ArrayList<>();

        Informe anonimo = Anonimizador.anonimizar(original, nombres);

        assertEquals("E01", anonimo.filas().get(0).get(0));
        assertEquals("E02", anonimo.filas().get(1).get(0));
        assertEquals(List.of("Ana López", "Luis Torres"), nombres);
        assertEquals("Ana López", original.filas().get(0).get(0));   // el reporte original queda igual
    }

    @Test
    void elTextoQueSeEnviaNoLlevaNingunNombre() {
        Informe original = informe(List.of("Empleado", "Departamento", "Turnos"), fila("Ana López", "Pediatría", 2));
        Informe anonimo = Anonimizador.anonimizar(original, new ArrayList<>());

        String texto = Anonimizador.aTexto(anonimo);

        assertFalse(texto.contains("Ana"));
        assertFalse(texto.contains("López"));
        assertTrue(texto.contains("E01"));
        assertTrue(texto.contains("Pediatría"));
    }

    @Test
    void losCodigosVuelvenASerNombresSinConfundirE1ConE10() {
        List<String> nombres = new ArrayList<>();
        for (int i = 1; i <= 10; i++) nombres.add("Persona" + i);

        String texto = Anonimizador.restaurar("E10 y E01 tienen más extras que E05. E99 no existe.", nombres);

        assertEquals("Persona10 y Persona1 tienen más extras que Persona5. E99 no existe.", texto);
    }

    @Test
    void losReportesSinColumnaEmpleadoNoSeCambian() {
        Informe cobertura = informe(List.of("Departamento", "Capacidad"), fila("Pediatría", 20));
        List<String> nombres = new ArrayList<>();

        assertSame(cobertura, Anonimizador.anonimizar(cobertura, nombres));
        assertTrue(nombres.isEmpty());
    }
}
