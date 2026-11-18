package com.hospital.gestion.chatbot;

import com.hospital.gestion.security.ContextoActual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Asistente de SIGPMAT. Abierto a cualquier usuario con sesión; las respuestas respetan su rol y su alcance. */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    public record Mensaje(@NotBlank @Size(max = 300, message = "El mensaje no puede pasar de 300 caracteres") String mensaje) { }

    private final ChatbotService servicio;
    private final ContextoActual contexto;

    public ChatbotController(ChatbotService servicio, ContextoActual contexto) {
        this.servicio = servicio;
        this.contexto = contexto;
    }

    @PostMapping("/preguntar")
    public ChatbotService.Respuesta preguntar(@Valid @RequestBody Mensaje m) {
        return servicio.responder(contexto.obtener(), m.mensaje());
    }
}
