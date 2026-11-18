package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Disponibilidad;
import com.hospital.gestion.repository.DisponibilidadRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disponibilidades")
public class DisponibilidadController extends CrudController<Disponibilidad, Integer> {

    private final DisponibilidadRepository disponibilidades;

    public DisponibilidadController(DisponibilidadRepository repo, EntityManager em) {
        super(repo, em, "Disponibilidad");
        this.disponibilidades = repo;
    }

    @Override
    protected void assignId(Disponibilidad d, Integer id) {
        d.setIdDisponi(id);
    }

    @Override
    protected Specification<Disponibilidad> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "empleado", "departamento", "idDepartamento");
        if (ctx.es(Grupos.RECEPCION)) return Alcance.valorEn(Grupos.rolesQueAtienden(), "empleado", "usuario", "rol");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Disponibilidad x) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return true;
        if (ctx.es(Grupos.JEFE)) return ctx.idDepartamento() != null && ctx.idDepartamento().equals(deptoDe(x.getEmpleado()));
        if (ctx.es(Grupos.RECEPCION)) return atiendeCitas(x.getEmpleado());
        return false;
    }

    @GetMapping("/empleado/{idEmpleado}")
    public List<Disponibilidad> porEmpleado(@PathVariable Integer idEmpleado) {
        return filtrar(disponibilidades.findByEmpleado_IdEmpleado(idEmpleado));
    }

    @GetMapping("/dia/{diaSemana}")
    public List<Disponibilidad> porDia(@PathVariable String diaSemana) {
        return filtrar(disponibilidades.findByDiaSemana(diaSemana));
    }
}
