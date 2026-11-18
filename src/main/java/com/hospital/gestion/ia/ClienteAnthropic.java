package com.hospital.gestion.ia;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente de la API Messages de Anthropic (POST /v1/messages con x-api-key y anthropic-version).
 * La clave se lee de la variable de entorno IA_API_KEY; nunca se envía a la página.
 */
@Service
public class ClienteAnthropic implements ProveedorIA {

    private static final String VERSION_API = "2023-06-01";
    private static final int MAX_TOKENS = 700;

    private final RestClient http;
    private final String apiKey;
    private final String modelo;
    private final String url;

    public ClienteAnthropic(@Value("${app.ia.api-key:}") String apiKey,
                            @Value("${app.ia.modelo:claude-haiku-5-5}") String modelo,
                            @Value("${app.ia.url:https://api.anthropic.com/v1/messages}") String url,
                            @Value("${app.ia.timeout-segundos:30}") int timeoutSegundos) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.modelo = modelo;
        this.url = url;
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(10));
        fabrica.setReadTimeout(Duration.ofSeconds(timeoutSegundos));
        this.http = RestClient.builder().requestFactory(fabrica).build();
    }

    @Override
    public boolean disponible() {
        return !apiKey.isBlank();
    }

    @Override
    public String modelo() {
        return modelo;
    }

    @Override
    public String completar(String sistema, String usuario) {
        JsonNode respuesta = http.post()
                .uri(url)
                .header("x-api-key", apiKey)
                .header("anthropic-version", VERSION_API)
                .contentType(MediaType.APPLICATION_JSON)
                .body(construirCuerpo(modelo, sistema, usuario))
                .retrieve()
                .body(JsonNode.class);
        return extraerTexto(respuesta);
    }

    static Map<String, Object> construirCuerpo(String modelo, String sistema, String usuario) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("model", modelo);
        cuerpo.put("max_tokens", MAX_TOKENS);
        cuerpo.put("system", sistema);
        cuerpo.put("messages", List.of(Map.of("role", "user", "content", usuario)));
        return cuerpo;
    }

    /** La respuesta trae una lista de bloques (content); se unen los de tipo texto. */
    static String extraerTexto(JsonNode respuesta) {
        if (respuesta == null || !respuesta.has("content") || !respuesta.get("content").isArray()) {
            throw new IllegalStateException("Respuesta inesperada del servicio de IA");
        }
        StringBuilder texto = new StringBuilder();
        for (JsonNode bloque : respuesta.get("content")) {
            if ("text".equals(bloque.path("type").asText())) texto.append(bloque.path("text").asText());
        }
        String resultado = texto.toString().trim();
        if (resultado.isEmpty()) throw new IllegalStateException("El servicio de IA no devolvió texto");
        return resultado;
    }
}
