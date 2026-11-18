package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Vacacion;
import com.hospital.gestion.repository.VacacionRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/vacaciones")
public class VacacionController extends CrudController<Vacacion, Integer> {

    private static final Set<String> ESTADOS = Set.of("Solicitado", "Aprobado", "Rechazado");
    private final VacacionRepository vacaciones;

    public VacacionController(VacacionRepository repo, EntityManager em) {
        super(repo, em, "Vacación");
        this.vacaciones = repo;
    }

    @Override
    protected void assignId(Vacacion v, Integer id) {
        v.setIdVacacion(id);
    }

    @Override
    protected Specification<Vacacion> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "empleado", "departamento", "idDepartamento");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Vacacion x) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return true;
        if (ctx.es(Grupos.JEFE)) return ctx.idDepartamento() != null && ctx.idDepartamento().equals(deptoDe(x.getEmpleado()));
        return false;
    }

    @GetMapping("/empleado/{idEmpleado}")
    public List<Vacacion> porEmpleado(@PathVariable Integer idEmpleado) {
        return filtrar(vacaciones.findByEmpleado_IdEmpleado(idEmpleado));
    }

    @GetMapping("/estado/{estado}")
    public List<Vacacion> porEstado(@PathVariable String estado) {
        return filtrar(vacaciones.findByEstadoVac(estado));
    }

    /** PATCH /api/vacaciones/3/estado?valor=Aprobado */
    @PatchMapping("/{id}/estado")
    public Vacacion cambiarEstado(@PathVariable Integer id, @RequestParam String valor) {
        if (!ESTADOS.contains(valor)) throw new IllegalArgumentException("Estado inválido. Use: " + ESTADOS);
        Vacacion v = find(id);
        verificarAlcance(v);
        noEsPropia(v.getEmpleado());
        v.setEstadoVac(valor);
        return saveAndReload(v);
    }
}
