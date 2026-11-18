package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;

import com.hospital.gestion.model.Paciente;
import com.hospital.gestion.repository.CitaRepository;
import com.hospital.gestion.repository.PacienteRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController extends CrudController<Paciente, Integer> {

    private final PacienteRepository pacientes;
    private final CitaRepository citas;

    public PacienteController(PacienteRepository repo, EntityManager em, CitaRepository citas) {
        super(repo, em, "Paciente");
        this.pacientes = repo;
        this.citas = citas;
    }

    @Override
    protected void assignId(Paciente p, Integer id) {
        p.setIdPaciente(id);
    }

    @Override
    protected Specification<Paciente> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RECEPCION)) return null;
        if (ctx.es(Grupos.MEDICO)) return Alcance.pacientesConCita("empleado", ctx.idEmpleado());
        if (ctx.es(Grupos.ENFERMERIA)) return Alcance.pacientesConCita("departamento", ctx.idDepartamento());
        return Alcance.ninguno();
    }

    /** Médico: pacientes de sus citas. Enfermería: pacientes con citas en su departamento. */
    @Override
    protected boolean enAlcance(Contexto ctx, Paciente p) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RECEPCION)) return true;
        if (p.getIdPaciente() == null) return false;
        if (ctx.es(Grupos.MEDICO)) {
            return ctx.idEmpleado() != null && citas.existsByPaciente_IdPacienteAndEmpleado_IdEmpleado(p.getIdPaciente(), ctx.idEmpleado());
        }
        if (ctx.es(Grupos.ENFERMERIA)) {
            return ctx.idDepartamento() != null && citas.existsByPaciente_IdPacienteAndDepartamento_IdDepartamento(p.getIdPaciente(), ctx.idDepartamento());
        }
        return false;
    }

    /** GET /api/pacientes/buscar?q=lopez  (nombre, apellido o documento) */
    @GetMapping("/buscar")
    public List<Paciente> buscar(@RequestParam String q) {
        return filtrar(pacientes.buscar(q));
    }
}
