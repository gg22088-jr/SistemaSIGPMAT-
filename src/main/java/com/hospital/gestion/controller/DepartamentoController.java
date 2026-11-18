package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Departamento;
import com.hospital.gestion.repository.DepartamentoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departamentos")
public class DepartamentoController extends CrudController<Departamento, Integer> {

    public DepartamentoController(DepartamentoRepository repo, EntityManager em) {
        super(repo, em, "Departamento");
    }

    @Override
    protected void assignId(Departamento d, Integer id) {
        d.setIdDepartamento(id);
    }

    @Override
    protected Specification<Departamento> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH, Grupos.RECEPCION)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "idDepartamento");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Departamento d) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH, Grupos.RECEPCION)) return true;
        return ctx.es(Grupos.JEFE) && ctx.idDepartamento() != null && ctx.idDepartamento().equals(d.getIdDepartamento());
    }
}
