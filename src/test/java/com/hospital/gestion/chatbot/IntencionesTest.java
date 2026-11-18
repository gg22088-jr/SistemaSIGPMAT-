package com.hospital.gestion.chatbot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntencionesTest {

    private static void espera(Intencion esperada, String pregunta) {
        assertEquals(esperada, Intenciones.detectar(pregunta), pregunta);
    }

    @Test
    void normalizaAcentosMayusculasYSignos() {
        assertEquals("cual es mi proximo turno", Intenciones.normalizar("¿Cuál es mi PRÓXIMO turno?"));
        assertEquals("contrasena", Intenciones.normalizar("contraseña"));
        assertEquals("", Intenciones.normalizar(null));
    }

    @Test
    void entiendeLasPreguntasSobreTurnos() {
        espera(Intencion.PROXIMO_TURNO, "¿Cuál es mi próximo turno?");
        espera(Intencion.PROXIMO_TURNO, "cuando me toca trabajar");
        espera(Intencion.TURNOS_HOY, "Turnos de hoy");
        espera(Intencion.TURNOS_HOY, "tengo turno hoy?");
        espera(Intencion.MIS_TURNOS, "mi horario");
        espera(Intencion.MIS_TURNOS, "mis turnos");
        espera(Intencion.DISPONIBILIDAD, "¿Cuál es mi disponibilidad?");
    }

    @Test
    void distingueMisSolicitudesDeLosPendientesYDeComoSolicitar() {
        espera(Intencion.VACACIONES, "mis vacaciones");
        espera(Intencion.PERMISOS, "estado de mis permisos");
        espera(Intencion.PERMISOS_PENDIENTES, "permisos pendientes");
        espera(Intencion.VACACIONES_PENDIENTES, "¿hay vacaciones por aprobar?");
        espera(Intencion.COMO_SOLICITAR, "¿Cómo solicito vacaciones?");
        espera(Intencion.COMO_SOLICITAR, "como puedo pedir un permiso");
    }

    @Test
    void entiendeOtrasPreguntas() {
        espera(Intencion.CITAS, "citas de hoy");
        espera(Intencion.CITAS, "mi agenda");
        espera(Intencion.CONTRASENA, "quiero cambiar mi contraseña");
        espera(Intencion.INCIDENCIAS, "mis incidencias");
        espera(Intencion.PERFIL, "mis datos");
        espera(Intencion.AYUDA, "ayuda");
        espera(Intencion.AYUDA, "¿qué puedes hacer?");
    }

    @Test
    void elSaludoSoloCuentaSiNoHayOtraPregunta() {
        espera(Intencion.SALUDO, "Hola");
        espera(Intencion.SALUDO, "buenos días");
        espera(Intencion.PROXIMO_TURNO, "hola, cuál es mi próximo turno");
        espera(Intencion.GRACIAS, "muchas gracias");
    }

    @Test
    void loQueNoEntiendeLoMarcaComoDesconocido() {
        espera(Intencion.DESCONOCIDA, "blablabla");
        espera(Intencion.DESCONOCIDA, "");
    }
}
