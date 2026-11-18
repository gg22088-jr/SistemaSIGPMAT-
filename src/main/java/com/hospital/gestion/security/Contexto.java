package com.hospital.gestion.security;

import java.util.Arrays;

/** Quién hace la petición: se calcula en el servidor a partir del token, nunca de datos enviados por la página. */
public record Contexto(Integer idUsuario, String grupo, Integer idEmpleado, Integer idDepartamento) {

    public boolean es(String... grupos) {
        return Arrays.asList(grupos).contains(grupo);
    }
}
