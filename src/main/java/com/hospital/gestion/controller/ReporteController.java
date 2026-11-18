package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;
import com.hospital.gestion.exception.AccesoDenegadoException;

import com.hospital.gestion.model.Reporte;
import com.hospital.gestion.repository.ReporteRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReporteController extends CrudController<Reporte, Integer> {

    private final ReporteRepository reportes;

    public ReporteController(ReporteRepository repo, EntityManager em) {
        super(repo, em, "Reporte");
        this.reportes = repo;
    }

    @Override
    protected void assignId(Reporte r, Integer id) {
        r.setIdReporte(id);
    }

    @Override
    protected Specification<Reporte> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR)) return null;
        return Alcance.campo(ctx.idUsuario(), "usuario", "idUsuario");
    }

    /** Administración y Dirección ven todo el historial; los demás, solo los reportes que generaron. */
    @Override
    protected boolean enAlcance(Contexto ctx, Reporte r) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR)) return true;
        return r.getUsuario() != null && ctx.idUsuario() != null && ctx.idUsuario().equals(r.getUsuario().getIdUsuario());
    }

    @Override
    protected void beforeCreate(Reporte r) {
        throw new AccesoDenegadoException("Los reportes se generan desde la sección Reportes");
    }

    @Override
    protected void beforeUpdate(Integer id, Reporte r) {
        throw new AccesoDenegadoException("Los reportes generados no se pueden modificar");
    }

    @GetMapping("/usuario/{idUsuario}")
    public List<Reporte> porUsuario(@PathVariable Integer idUsuario) {
        return filtrar(reportes.findByUsuario_IdUsuario(idUsuario));
    }

    @GetMapping("/tipo/{tipo}")
    public List<Reporte> porTipo(@PathVariable String tipo) {
        return filtrar(reportes.findByTipoReporte(tipo));
    }
}
