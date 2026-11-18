package com.hospital.gestion.exception;

/** El usuario está autenticado pero no puede ver o hacer esto (alcance de datos). Se responde 403. */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String message) {
        super(message);
    }
}
