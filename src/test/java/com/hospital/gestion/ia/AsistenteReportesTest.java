package com.hospital.gestion.ia;

import com.hospital.gestion.service.Informe;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsistenteReportesTest {

    private final ProveedorIA proveedor = mock(ProveedorIA.class);
    private final AsistenteReportes asistente = new AsistenteReportes(proveedor);

    private static Informe informe() {
        List<List<Object>> filas = new ArrayList<>();
        filas.add(new ArrayList<>(Arrays.asList("Ana López", "Pediatría", 10, 120.0, 90.0, 30.0, 96.0, 24.0, 20.0, 4.0, 6)));
        return new Informe("Horas Extras", "Reporte de horas extras", "Del 2026-11-01 al 2026-11-30",
                List.of("Empleado", "Departamento", "Turnos cumplidos", "Horas trabajadas", "Horas diurnas", "Horas nocturnas",
                        "Horas ordinarias", "Horas extras", "Extras diurnas", "Extras nocturnas", "Días con extras"),
                filas, Arrays.asList("TOTAL", "", 10, 120.0, 90.0, 30.0, 96.0, 24.0, 20.0, 4.0, 6), "nota");
    }

    @Test
    void sinClaveUsaElAnalisisBasicoYNoLlamaAlServicio() {
        when(proveedor.disponible()).thenReturn(false);

        AsistenteReportes.Resultado r = asistente.analizar(informe());

        assertEquals("local", r.modo());
        assertTrue(r.texto().contains("Resumen de horas extras"));
        assertNotNull(r.aviso());
        verify(proveedor, never()).completar(anyString(), anyString());
    }

    @Test
    void conClaveEnviaDatosAnonimosYDevuelveLosNombres() {
        when(proveedor.disponible()).thenReturn(true);
        when(proveedor.modelo()).thenReturn("claude-haiku-5-5");
        when(proveedor.completar(anyString(), anyString())).thenReturn("E01 concentra las horas extras del periodo.");

        AsistenteReportes.Resultado r = asistente.analizar(informe());

        assertEquals("ia", r.modo());
        assertEquals("claude-haiku-5-5", r.modelo());
        assertEquals("Ana López concentra las horas extras del periodo.", r.texto());

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(proveedor).completar(anyString(), enviado.capture());
        assertFalse(enviado.getValue().contains("Ana"), "no debe enviarse el nombre");
        assertTrue(enviado.getValue().contains("E01"));
    }

    @Test
    void siElServicioFallaSeUsaElAnalisisBasico() {
        when(proveedor.disponible()).thenReturn(true);
        when(proveedor.completar(anyString(), anyString())).thenThrow(new IllegalStateException("sin conexión"));

        AsistenteReportes.Resultado r = asistente.analizar(informe());

        assertEquals("local", r.modo());
        assertTrue(r.texto().contains("Resumen de horas extras"));
        assertTrue(r.aviso().contains("No se pudo contactar"));
    }
}
