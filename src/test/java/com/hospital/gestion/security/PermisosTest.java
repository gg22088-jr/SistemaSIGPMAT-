package com.hospital.gestion.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PermisosTest {

    private static Map<String, Boolean> de(String grupo, String recurso) {
        return Permisos.para(grupo).get(recurso);
    }

    @Test
    void elPersonalOperativoYElContadorNoAccedenAModulosGenerales() {
        // Farmacéutico, laboratorista, limpieza, etc.: solo su espacio laboral (/api/mi), nada de módulos generales
        for (String grupo : new String[]{Grupos.OPERATIVO, Grupos.CONTADOR}) {
            Permisos.para(grupo).forEach((recurso, p) -> {
                assertFalse(p.get("leer"), grupo + " no debe leer " + recurso);
                assertFalse(p.get("escribir"), grupo + " no debe escribir " + recurso);
                assertFalse(p.get("eliminar"), grupo + " no debe eliminar " + recurso);
            });
        }
    }

    @Test
    void elAdministradorYElDirectorTienenAccesoGeneralYPuedenAsignar() {
        for (String grupo : new String[]{Grupos.ADMIN, Grupos.DIRECTOR}) {
            for (String recurso : new String[]{"empleados", "turnos", "disponibilidades", "vacaciones", "permisos",
                    "incidencias", "pacientes", "citas", "departamentos", "especialidades", "reportes"}) {
                assertTrue(de(grupo, recurso).get("leer"), grupo + " debe leer " + recurso);
                assertTrue(de(grupo, recurso).get("escribir"), grupo + " debe poder crear y editar " + recurso);
                assertTrue(de(grupo, recurso).get("eliminar"), grupo + " debe poder eliminar " + recurso);
            }
        }
    }

    @Test
    void soloElAdministradorGestionaLasCuentasDeUsuario() {
        assertTrue(de(Grupos.ADMIN, "usuarios").get("escribir"));
        assertFalse(de(Grupos.DIRECTOR, "usuarios").get("leer"));
        assertFalse(de(Grupos.JEFE, "usuarios").get("leer"));
    }


    @Test
    void laJefaturaGestionaLoLaboralPeroNoVePacientesNiUsuarios() {
        assertTrue(de(Grupos.JEFE, "turnos").get("escribir"));
        assertTrue(de(Grupos.JEFE, "vacaciones").get("escribir"));
        assertFalse(de(Grupos.JEFE, "pacientes").get("leer"));
        assertFalse(de(Grupos.JEFE, "citas").get("leer"));
        assertFalse(de(Grupos.JEFE, "usuarios").get("leer"));
    }

    @Test
    void recursosHumanosGestionaLoLaboralSinAccederAPacientes() {
        assertTrue(de(Grupos.RRHH, "empleados").get("escribir"));
        assertTrue(de(Grupos.RRHH, "vacaciones").get("escribir"));
        assertTrue(de(Grupos.RRHH, "turnos").get("escribir"));
        assertFalse(de(Grupos.RRHH, "pacientes").get("leer"));
        assertFalse(de(Grupos.RRHH, "citas").get("leer"));
    }

    @Test
    void elMedicoVeSusCitasYPacientesPeroNoEditaPacientes() {
        assertTrue(de(Grupos.MEDICO, "citas").get("leer"));
        assertTrue(de(Grupos.MEDICO, "pacientes").get("leer"));
        assertFalse(de(Grupos.MEDICO, "pacientes").get("escribir"));
        assertFalse(de(Grupos.MEDICO, "citas").get("eliminar"));
        assertFalse(de(Grupos.MEDICO, "empleados").get("leer"));
        assertFalse(de(Grupos.MEDICO, "turnos").get("leer"));
    }

    @Test
    void enfermeriaSoloVePacientes() {
        assertTrue(de(Grupos.ENFERMERIA, "pacientes").get("leer"));
        assertFalse(de(Grupos.ENFERMERIA, "pacientes").get("escribir"));
        assertFalse(de(Grupos.ENFERMERIA, "citas").get("leer"));
        assertFalse(de(Grupos.ENFERMERIA, "empleados").get("leer"));
    }

    @Test
    void recepcionGestionaPacientesYCitasPeroNoLoLaboral() {
        assertTrue(de(Grupos.RECEPCION, "pacientes").get("escribir"));
        assertTrue(de(Grupos.RECEPCION, "citas").get("escribir"));
        assertFalse(de(Grupos.RECEPCION, "citas").get("eliminar"));
        assertFalse(de(Grupos.RECEPCION, "turnos").get("leer"));
        assertFalse(de(Grupos.RECEPCION, "vacaciones").get("leer"));
        assertFalse(de(Grupos.RECEPCION, "reportes").get("leer"));
    }

    @Test
    void todosLosRecursosTienenRegla() {
        for (String recurso : new String[]{"usuarios", "reportes", "departamentos", "especialidades", "empleados",
                "turnos", "disponibilidades", "vacaciones", "permisos", "incidencias", "pacientes", "citas"}) {
            assertTrue(Permisos.REGLAS.containsKey(recurso), "falta la regla de " + recurso);
        }
    }
}
