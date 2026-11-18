package com.hospital.gestion.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcultarDatosPersonalesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void quitaLosDatosPersonalesDelEmpleadoAunqueVengaDentroDeUnTurno() throws Exception {
        JsonNode arbol = mapper.readTree("""
                {"idTurno": 1, "empleado": {"idEmpleado": 3, "nombre": "Ana", "apellido": "López",
                 "documentoId": "DOC003", "telefono": "7001", "correo": "ana@hospital.com",
                 "estadoLaboral": "Incapacitado", "usuario": {"idUsuario": 3, "rol": "Pediatra"},
                 "cargo": "Pediatra"}}""");

        OcultarDatosPersonales.limpiar(arbol);

        JsonNode empleado = arbol.get("empleado");
        assertEquals("Ana", empleado.get("nombre").asText());
        assertEquals("Pediatra", empleado.get("cargo").asText());
        for (String campo : new String[]{"documentoId", "telefono", "correo", "estadoLaboral", "usuario"}) {
            assertFalse(empleado.has(campo), "debía ocultarse " + campo);
        }
    }

    @Test
    void tambienLimpiaUnaListaDeEmpleados() throws Exception {
        JsonNode arbol = mapper.readTree("""
                [{"idEmpleado": 1, "nombre": "A", "documentoId": "D1", "correo": "a@x"},
                 {"idEmpleado": 2, "nombre": "B", "documentoId": "D2", "correo": "b@x"}]""");

        OcultarDatosPersonales.limpiar(arbol);

        for (JsonNode e : arbol) {
            assertFalse(e.has("documentoId"));
            assertFalse(e.has("correo"));
            assertTrue(e.has("nombre"));
        }
    }

    @Test
    void noTocaLosObjetosQueNoSonEmpleados() throws Exception {
        JsonNode arbol = mapper.readTree("""
                {"usuario": {"idUsuario": 1, "nombreUsuario": "admin01"},
                 "paciente": {"idPaciente": 5, "documentoId": "PAC005", "telefono": "7801"}}""");

        OcultarDatosPersonales.limpiar(arbol);

        assertTrue(arbol.get("usuario").has("nombreUsuario"));
        assertTrue(arbol.get("paciente").has("documentoId"));
        assertTrue(arbol.get("paciente").has("telefono"));
    }
}
