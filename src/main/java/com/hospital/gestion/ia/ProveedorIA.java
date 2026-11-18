package com.hospital.gestion.ia;

/** Servicio de modelo de lenguaje. Está detrás de una interfaz para poder cambiar de proveedor o probarlo sin red. */
public interface ProveedorIA {

    /** ¿Hay una clave configurada? Sin clave se usa el análisis básico. */
    boolean disponible();

    String modelo();

    /** Envía las instrucciones (sistema) y el contenido (usuario) y devuelve el texto de la respuesta. */
    String completar(String sistema, String usuario);
}
