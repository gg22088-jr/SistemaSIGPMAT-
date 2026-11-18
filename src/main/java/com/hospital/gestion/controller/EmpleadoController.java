package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.repository.EmpleadoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empleados")
public class EmpleadoController extends CrudController<Empleado, Integer> {

    private final EmpleadoRepository empleados;

    public EmpleadoController(EmpleadoRepository repo, EntityManager em) {
        super(repo, em, "Empleado");
        this.empleados = repo;
    }

    @Override
    protected void assignId(Empleado e, Integer id) {
        e.setIdEmpleado(id);
    }

    @Override
    protected Specification<Empleado> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "departamento", "idDepartamento");
        if (ctx.es(Grupos.RECEPCION)) return Alcance.valorEn(Grupos.rolesQueAtienden(), "usuario", "rol");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Empleado e) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return true;
        if (ctx.es(Grupos.JEFE)) return ctx.idDepartamento() != null && ctx.idDepartamento().equals(deptoDe(e));
        if (ctx.es(Grupos.RECEPCION)) return atiendeCitas(e);
        return false;
    }

    @GetMapping("/departamento/{idDepartamento}")
    public List<Empleado> porDepartamento(@PathVariable Integer idDepartamento) {
        return filtrar(empleados.findByDepartamento_IdDepartamento(idDepartamento));
    }

    @GetMapping("/estado/{estadoLaboral}")
    public List<Empleado> porEstado(@PathVariable String estadoLaboral) {
        return filtrar(empleados.findByEstadoLaboral(estadoLaboral));
    }
}
