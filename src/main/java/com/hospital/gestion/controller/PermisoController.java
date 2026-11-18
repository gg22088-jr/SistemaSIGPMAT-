package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Permiso;
import com.hospital.gestion.repository.PermisoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/permisos")
public class PermisoController extends CrudController<Permiso, Integer> {

    private static final Set<String> ESTADOS = Set.of("Pendiente", "Aprobado", "Rechazado");
    private final PermisoRepository permisos;

    public PermisoController(PermisoRepository repo, EntityManager em) {
        super(repo, em, "Permiso");
        this.permisos = repo;
    }

    @Override
    protected void assignId(Permiso p, Integer id) {
        p.setIdPermiso(id);
    }

    @Override
    protected Specification<Permiso> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "empleado", "departamento", "idDepartamento");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Permiso x) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return true;
        if (ctx.es(Grupos.JEFE)) return ctx.idDepartamento() != null && ctx.idDepartamento().equals(deptoDe(x.getEmpleado()));
        return false;
    }

    @GetMapping("/empleado/{idEmpleado}")
    public List<Permiso> porEmpleado(@PathVariable Integer idEmpleado) {
        return filtrar(permisos.findByEmpleado_IdEmpleado(idEmpleado));
    }

    @GetMapping("/estado/{estado}")
    public List<Permiso> porEstado(@PathVariable String estado) {
        return filtrar(permisos.findByEstadoPer(estado));
    }

    /** PATCH /api/permisos/3/estado?valor=Aprobado */
    @PatchMapping("/{id}/estado")
    public Permiso cambiarEstado(@PathVariable Integer id, @RequestParam String valor) {
        if (!ESTADOS.contains(valor)) throw new IllegalArgumentException("Estado inválido. Use: " + ESTADOS);
        Permiso p = find(id);
        verificarAlcance(p);
        noEsPropia(p.getEmpleado());
        p.setEstadoPer(valor);
        return saveAndReload(p);
    }
}
