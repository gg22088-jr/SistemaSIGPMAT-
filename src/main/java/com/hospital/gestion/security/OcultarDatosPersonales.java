package com.hospital.gestion.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * Quita los datos personales de cada empleado (documento, teléfono, correo, usuario y estado laboral)
 * de TODAS las respuestas, incluso cuando el empleado viene dentro de un turno, una cita, etc.
 * Solo Administración, Dirección, Jefatura y Recursos Humanos los ven completos.
 */
@RestControllerAdvice
public class OcultarDatosPersonales implements ResponseBodyAdvice<Object> {

    private static final List<String> SENSIBLES = List.of("documentoId", "telefono", "correo", "usuario", "estadoLaboral");

    private final ObjectMapper mapper;

    public OcultarDatosPersonales(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || !debeOcultar()) return body;
        JsonNode arbol = mapper.valueToTree(body);
        limpiar(arbol);
        return arbol;
    }

    private static boolean debeOcultar() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().noneMatch(g ->
                g.getAuthority().equals("ROLE_" + Grupos.ADMIN) || g.getAuthority().equals("ROLE_" + Grupos.JEFE)
                        || g.getAuthority().equals("ROLE_" + Grupos.DIRECTOR) || g.getAuthority().equals("ROLE_" + Grupos.RRHH));
    }

    /** Recorre el JSON y, en cada objeto que sea un empleado, borra los campos sensibles. */
    public static void limpiar(JsonNode nodo) {
        if (nodo.isObject()) {
            ObjectNode objeto = (ObjectNode) nodo;
            if (objeto.has("idEmpleado") && objeto.has("documentoId")) objeto.remove(SENSIBLES);
            objeto.elements().forEachRemaining(OcultarDatosPersonales::limpiar);
        } else if (nodo.isArray()) {
            nodo.forEach(OcultarDatosPersonales::limpiar);
        }
    }
}
