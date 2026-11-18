package com.hospital.gestion.service;

import java.util.List;

/** Resultado de un reporte: una tabla genérica que sirve para la página, Excel y PDF. */
public record Informe(String tipo, String titulo, String periodo,
                      List<String> columnas, List<List<Object>> filas,
                      List<Object> totales, String nota) { }
