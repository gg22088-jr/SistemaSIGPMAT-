package com.hospital.gestion.ia;

import com.hospital.gestion.service.Informe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Analiza un reporte ya calculado. Con clave de IA lo redacta un modelo de lenguaje (con las personas anonimizadas);
 * sin clave, o si el servicio falla, se usa el análisis básico por reglas.
 */
@Service
public class AsistenteReportes {

    public record Resultado(String texto, String modo, String modelo, String aviso) { }

    static final String INSTRUCCIONES = """
            Eres un analista de recursos humanos de un hospital. Recibes un reporte ya calculado en forma de tabla \
            (asistencia, cobertura u horas extras). Las personas aparecen con códigos anonimizados (E01, E02...). \
            Escribe en español un análisis breve (máximo 180 palabras) con tres partes: 1) hallazgos principales, \
            2) posibles riesgos y 3) dos o tres recomendaciones prácticas. Usa solo los datos de la tabla: no inventes \
            cifras ni causas. Cuando menciones a una persona, usa su código tal como aparece. No hagas juicios sobre \
            las personas ni diagnósticos de salud: describe los datos. Si hay pocos datos, dilo.""";

    private static final Logger log = LoggerFactory.getLogger(AsistenteReportes.class);

    private final ProveedorIA proveedor;

    public AsistenteReportes(ProveedorIA proveedor) {
        this.proveedor = proveedor;
    }

    public Resultado analizar(Informe informe) {
        if (!proveedor.disponible()) {
            return new Resultado(AnalisisLocal.analizar(informe), "local", null,
                    "Análisis básico automático (sin IA). Para usar IA, el administrador del servidor debe configurar la clave IA_API_KEY.");
        }
        List<String> nombres = new ArrayList<>();
        Informe anonimo = Anonimizador.anonimizar(informe, nombres);
        try {
            String texto = proveedor.completar(INSTRUCCIONES, Anonimizador.aTexto(anonimo));
            return new Resultado(Anonimizador.restaurar(texto, nombres), "ia", proveedor.modelo(), null);
        } catch (RuntimeException e) {
            log.warn("No se pudo obtener el análisis del servicio de IA: {}", e.getMessage());
            return new Resultado(AnalisisLocal.analizar(informe), "local", null,
                    "No se pudo contactar al servicio de IA; se muestra el análisis básico automático.");
        }
    }
}
