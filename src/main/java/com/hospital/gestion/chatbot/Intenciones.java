package com.hospital.gestion.chatbot;

import java.text.Normalizer;

/** Entiende preguntas sencillas buscando palabras clave (sin acentos ni mayúsculas). El orden de las reglas importa. */
public final class Intenciones {

    private Intenciones() { }

    /** Minúsculas, sin acentos ni signos, con espacios simples: "¿Cuál es mi PRÓXIMO turno?" -> "cual es mi proximo turno". */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    public static Intencion detectar(String texto) {
        String t = " " + normalizar(texto) + " ";

        if (tiene(t, "como solicit", "como pid", "como puedo pedir", "como hago para pedir", "solicitar vacaciones",
                "solicitar permiso", "solicitar un permiso", "pedir vacaciones", "pedir permiso", "pedir un permiso")) {
            return Intencion.COMO_SOLICITAR;
        }
        if (tiene(t, "contrasena", "password", " clave ")) return Intencion.CONTRASENA;

        boolean pendiente = tiene(t, "pendiente", "por aprobar", "sin aprobar", "por resolver");
        if (pendiente && tiene(t, "vacacion")) return Intencion.VACACIONES_PENDIENTES;
        if (pendiente && tiene(t, "permiso")) return Intencion.PERMISOS_PENDIENTES;

        if (tiene(t, " cita ", " citas ", "agenda")) return Intencion.CITAS;
        if (tiene(t, "proximo turno", "siguiente turno", "cuando trabajo", "cuando me toca", "cuando entro", "mi proximo")) {
            return Intencion.PROXIMO_TURNO;
        }
        if (tiene(t, "turno de hoy", "turnos de hoy", "turno hoy", "turnos hoy", "trabajo hoy", "hoy trabajo",
                "quien trabaja hoy", "tengo turno hoy", "me toca hoy")) {
            return Intencion.TURNOS_HOY;
        }
        if (tiene(t, "turno", "horario")) return Intencion.MIS_TURNOS;
        if (tiene(t, "disponibilidad")) return Intencion.DISPONIBILIDAD;
        if (tiene(t, "vacacion")) return Intencion.VACACIONES;
        if (tiene(t, "permiso")) return Intencion.PERMISOS;
        if (tiene(t, "incidencia", "retraso", "inasistencia", " faltas ")) return Intencion.INCIDENCIAS;
        if (tiene(t, "mi perfil", "mis datos", "mi cargo", "mi departamento", "quien soy", "mi cuenta", "mi informacion")) {
            return Intencion.PERFIL;
        }
        if (tiene(t, "ayuda", "que puedes", "que sabes", "que haces", "opciones", " menu ", "comandos", "para que sirves")) {
            return Intencion.AYUDA;
        }
        if (tiene(t, "gracias", "adios", " chao ", "hasta luego", "nos vemos")) return Intencion.GRACIAS;
        if (tiene(t, " hola ", "buenos dias", "buenas tardes", "buenas noches", " buenas ", " hey ", "saludos")) {
            return Intencion.SALUDO;
        }
        return Intencion.DESCONOCIDA;
    }

    private static boolean tiene(String texto, String... frases) {
        for (String f : frases) if (texto.contains(f)) return true;
        return false;
    }
}
