package com.hospital.gestion.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GruposTest {

    private static final Map<String, String> ESPERADO = Map.ofEntries(
            Map.entry("Administrador", Grupos.ADMIN), Map.entry("Director", Grupos.DIRECTOR),
            Map.entry("Jefe de Área", Grupos.JEFE), Map.entry("Recursos Humanos", Grupos.RRHH),
            Map.entry("Personal Médico", Grupos.MEDICO), Map.entry("Odontólogo", Grupos.MEDICO),
            Map.entry("Fisioterapeuta", Grupos.MEDICO), Map.entry("Anestesiólogo", Grupos.MEDICO),
            Map.entry("Cirujano", Grupos.MEDICO), Map.entry("Pediatra", Grupos.MEDICO), Map.entry("Cardiólogo", Grupos.MEDICO),
            Map.entry("Enfermero", Grupos.ENFERMERIA), Map.entry("Auxiliar de Enfermería", Grupos.ENFERMERIA),
            Map.entry("Recepcionista", Grupos.RECEPCION), Map.entry("Secretaria", Grupos.RECEPCION),
            Map.entry("Contador", Grupos.CONTADOR),
            Map.entry("Farmacéutico", Grupos.OPERATIVO), Map.entry("Laboratorista", Grupos.OPERATIVO),
            Map.entry("Radiólogo", Grupos.OPERATIVO), Map.entry("Nutricionista", Grupos.OPERATIVO),
            Map.entry("Trabajador Social", Grupos.OPERATIVO), Map.entry("Conserje", Grupos.OPERATIVO),
            Map.entry("Personal de Limpieza", Grupos.OPERATIVO), Map.entry("Motorista", Grupos.OPERATIVO),
            Map.entry("Vigilante", Grupos.OPERATIVO), Map.entry("Técnico de Mantenimiento", Grupos.OPERATIVO),
            Map.entry("Cocinero", Grupos.OPERATIVO), Map.entry("Lavandería", Grupos.OPERATIVO));

    @Test
    void cadaUnoDeLos28RolesTieneSuGrupo() {
        assertEquals(28, ESPERADO.size());
        assertEquals(28, Grupos.todosLosRoles().size());
        ESPERADO.forEach((rol, grupo) -> assertEquals(grupo, Grupos.de(rol), rol));
    }

    @Test
    void elFarmaceuticoNoEstaEnUnGrupoClinico() {
        assertEquals(Grupos.OPERATIVO, Grupos.de("Farmacéutico"));
    }

    @Test
    void unRolDesconocidoCaeEnElGrupoMasRestringido() {
        assertEquals(Grupos.OPERATIVO, Grupos.de("Rol inventado"));
    }

    @Test
    void losRolesQueAtiendenCitasSonLosMedicosYLosJefesDeArea() {
        assertEquals(8, Grupos.rolesQueAtienden().size());
        assertTrue(Grupos.rolesQueAtienden().contains("Pediatra"));
        assertTrue(Grupos.rolesQueAtienden().contains("Jefe de Área"));
    }
}
