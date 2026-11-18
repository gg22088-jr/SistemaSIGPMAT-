package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Incidencia;
import com.hospital.gestion.repository.IncidenciaRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incidencias")
public class IncidenciaController extends CrudController<Incidencia, Integer> {

    private final IncidenciaRepository incidencias;

    public IncidenciaController(IncidenciaRepository repo, EntityManager em) {
        super(repo, em, "Incidencia");
        this.incidencias = repo;
    }

    @Override
    protected void assignId(Incidencia i, Integer id) {
        i.setIdIncidencia(id);
    }

    @Override
    protected Specification<Incidencia> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "empleado", "departamento", "idDepartamento");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Incidencia x) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return true;
        if (ctx.es(Grupos.JEFE)) return ctx.idDepartamento() != null && ctx.idDepartamento().equals(deptoDe(x.getEmpleado()));
        return false;
    }

    @GetMapping("/empleado/{idEmpleado}")
    public List<Incidencia> porEmpleado(@PathVariable Integer idEmpleado) {
        return filtrar(incidencias.findByEmpleado_IdEmpleado(idEmpleado));
    }

    @GetMapping("/tipo/{tipo}")
    public List<Incidencia> porTipo(@PathVariable String tipo) {
        return filtrar(incidencias.findByTipoIncidencia(tipo));
    }
}
