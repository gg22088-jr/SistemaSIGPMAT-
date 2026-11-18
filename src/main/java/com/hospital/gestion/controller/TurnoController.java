package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Turno;
import com.hospital.gestion.repository.TurnoRepository;
import com.hospital.gestion.service.ReglasAgenda;
import jakarta.persistence.EntityManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/turnos")
public class TurnoController extends CrudController<Turno, Integer> {

    private static final Set<String> ESTADOS = Set.of("Programado", "Cumplido", "Cancelado");
    private final TurnoRepository turnos;
    private final ReglasAgenda agenda;

    public TurnoController(TurnoRepository repo, EntityManager em, ReglasAgenda agenda) {
        super(repo, em, "Turno");
        this.turnos = repo;
        this.agenda = agenda;
    }

    @Override
    protected void assignId(Turno t, Integer id) {
        t.setIdTurno(id);
    }

    @Override
    protected Specification<Turno> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return null;
        if (ctx.es(Grupos.JEFE)) return Alcance.campo(ctx.idDepartamento(), "departamento", "idDepartamento");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Turno t) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RRHH)) return true;
        return ctx.es(Grupos.JEFE) && ctx.idDepartamento() != null && t.getDepartamento() != null
                && ctx.idDepartamento().equals(t.getDepartamento().getIdDepartamento());
    }

    @Override
    protected void beforeCreate(Turno t) {
        agenda.validarTurno(t, null);
    }

    @Override
    protected void beforeUpdate(Integer id, Turno t) {
        agenda.validarTurno(t, id);
    }

    @GetMapping("/empleado/{idEmpleado}")
    public List<Turno> porEmpleado(@PathVariable Integer idEmpleado) {
        return filtrar(turnos.findByEmpleado_IdEmpleado(idEmpleado));
    }

    @GetMapping("/departamento/{idDepartamento}")
    public List<Turno> porDepartamento(@PathVariable Integer idDepartamento) {
        return filtrar(turnos.findByDepartamento_IdDepartamento(idDepartamento));
    }

    @GetMapping("/fecha/{fecha}")
    public List<Turno> porFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return filtrar(turnos.findByFecha(fecha));
    }

    @GetMapping("/rango")
    public List<Turno> porRango(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return filtrar(turnos.findByFechaBetween(desde, hasta));
    }

    /** PATCH /api/turnos/5/estado?valor=Cumplido */
    @PatchMapping("/{id}/estado")
    public Turno cambiarEstado(@PathVariable Integer id, @RequestParam String valor) {
        if (!ESTADOS.contains(valor)) throw new IllegalArgumentException("Estado inválido. Use: " + ESTADOS);
        Turno t = find(id);
        verificarAlcance(t);
        t.setEstadoTurno(valor);
        agenda.validarTurno(t, id);   // al reactivar un turno cancelado se revisan de nuevo las reglas
        return saveAndReload(t);
    }
}
