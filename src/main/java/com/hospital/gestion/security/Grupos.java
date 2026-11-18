package com.hospital.gestion.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Agrupa los 28 roles de la tabla Usuario en 9 grupos de permisos. Un rol desconocido cae en OPERATIVO (el más restringido). */
public final class Grupos {
    public static final String ADMIN = "ADMIN", DIRECTOR = "DIRECTOR", JEFE = "JEFE", RRHH = "RRHH",
            MEDICO = "MEDICO", ENFERMERIA = "ENFERMERIA", RECEPCION = "RECEPCION",
            CONTADOR = "CONTADOR", OPERATIVO = "OPERATIVO";

    private static final Map<String, String> POR_ROL = Map.ofEntries(
        Map.entry("Administrador", ADMIN),
        Map.entry("Director", DIRECTOR),
        Map.entry("Jefe de Área", JEFE),
        Map.entry("Recursos Humanos", RRHH),
        Map.entry("Personal Médico", MEDICO), Map.entry("Odontólogo", MEDICO), Map.entry("Fisioterapeuta", MEDICO),
        Map.entry("Anestesiólogo", MEDICO), Map.entry("Cirujano", MEDICO), Map.entry("Pediatra", MEDICO),
        Map.entry("Cardiólogo", MEDICO),
        Map.entry("Enfermero", ENFERMERIA), Map.entry("Auxiliar de Enfermería", ENFERMERIA),
        Map.entry("Recepcionista", RECEPCION), Map.entry("Secretaria", RECEPCION),
        Map.entry("Contador", CONTADOR),
        Map.entry("Farmacéutico", OPERATIVO), Map.entry("Laboratorista", OPERATIVO), Map.entry("Radiólogo", OPERATIVO),
        Map.entry("Nutricionista", OPERATIVO), Map.entry("Trabajador Social", OPERATIVO), Map.entry("Conserje", OPERATIVO),
        Map.entry("Personal de Limpieza", OPERATIVO), Map.entry("Motorista", OPERATIVO), Map.entry("Vigilante", OPERATIVO),
        Map.entry("Técnico de Mantenimiento", OPERATIVO), Map.entry("Cocinero", OPERATIVO), Map.entry("Lavandería", OPERATIVO)
    );

    private Grupos() { }

    public static String de(String rol) {
        return POR_ROL.getOrDefault(rol, OPERATIVO);
    }

    /** Roles que pertenecen a un grupo. */
    public static List<String> rolesDe(String grupo) {
        List<String> roles = new ArrayList<>();
        POR_ROL.forEach((rol, g) -> { if (g.equals(grupo)) roles.add(rol); });
        return roles;
    }

    /** Roles que pueden atender citas: el grupo médico y los Jefes de Área (que también ejercen). */
    public static List<String> rolesQueAtienden() {
        List<String> roles = rolesDe(MEDICO);
        roles.add("Jefe de Área");
        return roles;
    }

    public static List<String> todosLosRoles() {
        return new ArrayList<>(POR_ROL.keySet());
    }
}
