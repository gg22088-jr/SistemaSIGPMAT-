package com.hospital.gestion.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteAnthropicTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void elCuerpoDeLaPeticionTieneElFormatoDeLaApiMessages() {
        Map<String, Object> cuerpo = ClienteAnthropic.construirCuerpo("claude-haiku-5-5", "instrucciones", "datos");

        assertEquals("claude-haiku-5-5", cuerpo.get("model"));
        assertEquals("instrucciones", cuerpo.get("system"));
        assertTrue(((Number) cuerpo.get("max_tokens")).intValue() > 0);
        List<?> mensajes = (List<?>) cuerpo.get("messages");
        Map<?, ?> primero = (Map<?, ?>) mensajes.get(0);
        assertEquals("user", primero.get("role"));
        assertEquals("datos", primero.get("content"));
    }

    @Test
    void seUnenLosBloquesDeTextoDeLaRespuesta() throws Exception {
        String respuesta = "{\"content\":[{\"type\":\"text\",\"text\":\"Hola\"},{\"type\":\"text\",\"text\":\" mundo\"}]}";
        assertEquals("Hola mundo", ClienteAnthropic.extraerTexto(json.readTree(respuesta)));
    }

    @Test
    void unaRespuestaSinTextoSeRechaza() throws Exception {
        assertThrows(IllegalStateException.class, () -> ClienteAnthropic.extraerTexto(json.readTree("{\"error\":\"x\"}")));
        assertThrows(IllegalStateException.class, () -> ClienteAnthropic.extraerTexto(json.readTree("{\"content\":[]}")));
        assertThrows(IllegalStateException.class, () -> ClienteAnthropic.extraerTexto(null));
    }

    @Test
    void sinClaveNoEstaDisponible() {
        assertFalse(new ClienteAnthropic("", "modelo", "http://localhost", 5).disponible());
        assertFalse(new ClienteAnthropic("   ", "modelo", "http://localhost", 5).disponible());
        assertTrue(new ClienteAnthropic("sk-ant-prueba", "modelo", "http://localhost", 5).disponible());
    }
}
