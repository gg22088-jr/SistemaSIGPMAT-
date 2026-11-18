package com.hospital.gestion.security;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.hospital.gestion.security.Grupos.*;

/**
 * Matriz de permisos por recurso: quién puede leer, escribir (crear/editar) y eliminar.
 * Es la única fuente de verdad: la usa SecurityConfig y la página (vía /api/auth/me).
 * Esto controla el ACCESO al recurso; el ALCANCE por fila (propios, departamento, asignados)
 * lo aplica cada controlador con los métodos alcance() y enAlcance().
 * Cada persona consulta lo suyo (horario, vacaciones, permisos...) por /api/mi/**, que no depende de esta matriz.
 *
 * Administrador y Director tienen acceso general y pueden asignar y gestionar (turnos, vacaciones, permisos,
 * incidencias, empleados, pacientes, citas...). La diferencia: solo el Administrador gestiona las cuentas de usuario.
 */
public final class Permisos {
    public record Regla(String[] leer, String[] escribir, String[] eliminar) { }

    private static final String[] A = {ADMIN};
    /** Acceso general: Administrador y Director. */
    private static final String[] DIRECCION = {ADMIN, DIRECTOR};
    private static final String[] GESTION = {ADMIN, DIRECTOR, JEFE, RRHH};
    private static final String[] DIRECCION_RRHH = {ADMIN, DIRECTOR, RRHH};

    public static final Map<String, Regla> REGLAS = new LinkedHashMap<>();

    static {
        REGLAS.put("usuarios", new Regla(A, A, A));
        REGLAS.put("reportes", new Regla(GESTION, GESTION, DIRECCION));
        REGLAS.put("departamentos", new Regla(new String[]{ADMIN, DIRECTOR, JEFE, RRHH, RECEPCION}, DIRECCION, DIRECCION));
        REGLAS.put("especialidades", new Regla(DIRECCION_RRHH, DIRECCION, DIRECCION));
        REGLAS.put("empleados", new Regla(new String[]{ADMIN, DIRECTOR, JEFE, RRHH, RECEPCION}, DIRECCION_RRHH, DIRECCION));
        REGLAS.put("turnos", new Regla(GESTION, GESTION, DIRECCION_RRHH));
        REGLAS.put("disponibilidades", new Regla(new String[]{ADMIN, DIRECTOR, JEFE, RRHH, RECEPCION}, GESTION, DIRECCION_RRHH));
        REGLAS.put("vacaciones", new Regla(GESTION, GESTION, DIRECCION_RRHH));
        REGLAS.put("permisos", new Regla(GESTION, GESTION, DIRECCION_RRHH));
        REGLAS.put("incidencias", new Regla(GESTION, GESTION, DIRECCION_RRHH));
        REGLAS.put("pacientes", new Regla(new String[]{ADMIN, DIRECTOR, MEDICO, ENFERMERIA, RECEPCION},
                new String[]{ADMIN, DIRECTOR, RECEPCION}, DIRECCION));
        REGLAS.put("citas", new Regla(new String[]{ADMIN, DIRECTOR, MEDICO, RECEPCION},
                new String[]{ADMIN, DIRECTOR, MEDICO, RECEPCION}, DIRECCION));
    }

    private Permisos() { }

    /** Permisos de un grupo, en el formato que consume la página. */
    public static Map<String, Map<String, Boolean>> para(String grupo) {
        Map<String, Map<String, Boolean>> out = new LinkedHashMap<>();
        REGLAS.forEach((recurso, r) -> {
            Map<String, Boolean> p = new LinkedHashMap<>();
            p.put("leer", Arrays.asList(r.leer()).contains(grupo));
            p.put("escribir", Arrays.asList(r.escribir()).contains(grupo));
            p.put("eliminar", Arrays.asList(r.eliminar()).contains(grupo));
            out.put(recurso, p);
        });
        return out;
    }
}
