package com.hospital.gestion.controller;

import com.hospital.gestion.exception.AccesoDenegadoException;
import com.hospital.gestion.ia.AsistenteReportes;
import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Reporte;
import com.hospital.gestion.model.Usuario;
import com.hospital.gestion.repository.ReporteRepository;
import com.hospital.gestion.repository.UsuarioRepository;
import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.ContextoActual;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.service.ExportadorExcel;
import com.hospital.gestion.service.ExportadorPdf;
import com.hospital.gestion.service.Informe;
import com.hospital.gestion.service.InformeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Generación y exportación de reportes (misma ruta base que el CRUD de Reporte, que guarda el historial).
 *   POST /api/reportes/generar/{asistencia|cobertura|horas-extras}?desde=&hasta=&departamento=&jornada=
 *   GET  /api/reportes/exportar/{tipo}?formato=xlsx|pdf&desde=&hasta=&departamento=&jornada=
 * Alcance: la Jefatura solo obtiene reportes de su departamento (se fuerza en el servidor) y
 * Recursos Humanos solo de asistencia y cobertura.
 */
@RestController
@RequestMapping("/api/reportes")
public class InformeController {

    private final InformeService informes;
    private final ReporteRepository reportes;
    private final UsuarioRepository usuarios;
    private final ContextoActual contexto;
    private final AsistenteReportes asistente;

    public InformeController(InformeService informes, ReporteRepository reportes,
                             UsuarioRepository usuarios, ContextoActual contexto,
                             AsistenteReportes asistente) {
        this.informes = informes;
        this.reportes = reportes;
        this.usuarios = usuarios;
        this.contexto = contexto;
        this.asistente = asistente;
    }

    /** Calcula el reporte y deja constancia en la tabla Reporte (quién lo generó y con qué parámetros). */
    @PostMapping("/generar/{tipo}")
    @Transactional
    public Informe generar(@PathVariable String tipo,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                           @RequestParam(required = false) Integer departamento,
                           @RequestParam(defaultValue = "8") double jornada,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nocheDesde,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nocheHasta,
                           Authentication auth) {
        Informe informe = informes.generar(tipo, desde, hasta, departamentoPermitido(tipo, departamento), jornada, nocheDesde, nocheHasta);
        Usuario u = usuarios.findByNombreUsuario(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        Reporte r = new Reporte();
        r.setUsuario(u);
        r.setTipoReporte(InformeService.tipoReporte(tipo));
        String parametros = informe.periodo();
        r.setParametros(parametros.length() > 255 ? parametros.substring(0, 255) : parametros);
        reportes.save(r);
        return informe;
    }

    @GetMapping("/exportar/{tipo}")
    public ResponseEntity<byte[]> exportar(@PathVariable String tipo,
                                           @RequestParam String formato,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                           @RequestParam(required = false) Integer departamento,
                                           @RequestParam(defaultValue = "8") double jornada,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nocheDesde,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nocheHasta) throws IOException {
        if (!"pdf".equals(formato) && !"xlsx".equals(formato)) {
            throw new IllegalArgumentException("Formato inválido: use pdf o xlsx");
        }
        Informe informe = informes.generar(tipo, desde, hasta, departamentoPermitido(tipo, departamento), jornada, nocheDesde, nocheHasta);
        boolean pdf = "pdf".equals(formato);
        byte[] datos = pdf ? ExportadorPdf.generar(informe) : ExportadorExcel.generar(informe);
        String mime = pdf ? "application/pdf" : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mime));
        headers.setContentDisposition(ContentDisposition.attachment().filename("reporte-" + tipo + "." + formato).build());
        return ResponseEntity.ok().headers(headers).body(datos);
    }

    /**
     * Análisis en lenguaje natural del reporte. Mismo alcance que generar/exportar. Con clave de IA lo redacta un modelo
     * (con las personas anonimizadas); sin clave, usa un análisis básico por reglas.
     */
    @PostMapping("/analizar/{tipo}")
    public AsistenteReportes.Resultado analizar(@PathVariable String tipo,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                @RequestParam(required = false) Integer departamento,
                                                @RequestParam(defaultValue = "8") double jornada,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nocheDesde,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime nocheHasta) {
        Informe informe = informes.generar(tipo, desde, hasta, departamentoPermitido(tipo, departamento), jornada, nocheDesde, nocheHasta);
        return asistente.analizar(informe);
    }

    /** Aplica el alcance del usuario: Jefatura = su departamento; Recursos Humanos sin horas extras. */
    private Integer departamentoPermitido(String tipo, Integer pedido) {
        Contexto ctx = contexto.obtener();
        if (ctx.es(Grupos.RRHH) && InformeService.HORAS_EXTRAS.equals(tipo)) {
            throw new AccesoDenegadoException("Recursos Humanos solo genera reportes de asistencia y cobertura");
        }
        if (ctx.es(Grupos.JEFE)) {
            if (ctx.idDepartamento() == null) {
                throw new AccesoDenegadoException("Su usuario no tiene un departamento asignado");
            }
            return ctx.idDepartamento();
        }
        return pedido;
    }
}
