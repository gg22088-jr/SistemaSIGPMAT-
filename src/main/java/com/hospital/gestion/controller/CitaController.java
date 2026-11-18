package com.hospital.gestion.controller;

import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Alcance;
import org.springframework.data.jpa.domain.Specification;
import com.hospital.gestion.exception.AccesoDenegadoException;

import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Cita;
import com.hospital.gestion.repository.CitaRepository;
import com.hospital.gestion.service.ReglasAgenda;
import jakarta.persistence.EntityManager;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/citas")
public class CitaController extends CrudController<Cita, Integer> {

    private static final Set<String> ESTADOS = Set.of("Programada", "Atendida", "Cancelada", "No asistió");
    private final CitaRepository citas;
    private final ReglasAgenda agenda;

    public CitaController(CitaRepository repo, EntityManager em, ReglasAgenda agenda) {
        super(repo, em, "Cita");
        this.citas = repo;
        this.agenda = agenda;
    }

    @Override
    protected void assignId(Cita c, Integer id) {
        c.setIdCita(id);
    }

    @Override
    protected Specification<Cita> alcance(Contexto ctx) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RECEPCION)) return null;
        if (ctx.es(Grupos.MEDICO)) return Alcance.campo(ctx.idEmpleado(), "empleado", "idEmpleado");
        return Alcance.ninguno();
    }

    @Override
    protected boolean enAlcance(Contexto ctx, Cita c) {
        if (ctx.es(Grupos.ADMIN, Grupos.DIRECTOR, Grupos.RECEPCION)) return true;
        return ctx.es(Grupos.MEDICO) && ctx.idEmpleado() != null && c.getEmpleado() != null
                && ctx.idEmpleado().equals(c.getEmpleado().getIdEmpleado());
    }

    /** El médico no crea ni edita citas: solo las marca como Atendida o No asistió. */
    private void denegarSiMedico() {
        if (contexto.obtener().es(Grupos.MEDICO)) {
            throw new AccesoDenegadoException("El médico solo puede marcar sus citas como Atendida o No asistió");
        }
    }

    @Override
    protected void beforeCreate(Cita c) {
        denegarSiMedico();
        if ("Programada".equals(c.getEstadoCita()) && c.getFecha().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("No se puede programar una cita en una fecha pasada");
        }
        validar(c, -1);
    }

    @Override
    protected void beforeUpdate(Integer id, Cita c) {
        denegarSiMedico();
        validar(c, id);
    }

    /** Reglas: médico activo, y sin cruce de horario para el médico ni para el paciente. */
    private void validar(Cita c, Integer excluir) {
        Integer idMedico = c.getEmpleado().getIdEmpleado();
        Integer idPaciente = c.getPaciente().getIdPaciente();
        if (idMedico == null || idPaciente == null) {
            throw new IllegalArgumentException("Debe indicar el id del paciente y del médico");
        }
        List<String> estado = em.createQuery(
                "select e.estadoLaboral from Empleado e where e.idEmpleado = :id", String.class)
                .setParameter("id", idMedico).getResultList();
        if (estado.isEmpty()) {
            throw new ResourceNotFoundException("Médico con id " + idMedico + " no encontrado");
        }
        if (!"Activo".equals(estado.get(0))) {
            throw new IllegalArgumentException("El médico no está activo (estado: " + estado.get(0) + ")");
        }
        if (!"Programada".equals(c.getEstadoCita())) return;

        agenda.validarCita(c);   // vacaciones, permisos y disponibilidad del médico

        if (citas.haySolapeMedico(idMedico, c.getFecha(), c.getHoraInicio(), c.getHoraFin(), excluir)) {
            throw new IllegalArgumentException("El médico ya tiene una cita programada en ese horario");
        }
        if (citas.haySolapePaciente(idPaciente, c.getFecha(), c.getHoraInicio(), c.getHoraFin(), excluir)) {
            throw new IllegalArgumentException("El paciente ya tiene una cita programada en ese horario");
        }
    }

    @GetMapping("/paciente/{idPaciente}")
    public List<Cita> porPaciente(@PathVariable Integer idPaciente) {
        return filtrar(citas.findByPaciente_IdPaciente(idPaciente));
    }

    @GetMapping("/medico/{idEmpleado}")
    public List<Cita> porMedico(@PathVariable Integer idEmpleado) {
        return filtrar(citas.findByEmpleado_IdEmpleado(idEmpleado));
    }

    @GetMapping("/fecha/{fecha}")
    public List<Cita> porFecha(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return filtrar(citas.findByFecha(fecha));
    }

    @GetMapping("/estado/{estado}")
    public List<Cita> porEstado(@PathVariable String estado) {
        return filtrar(citas.findByEstadoCita(estado));
    }

    /** PATCH /api/citas/5/estado?valor=Atendida */
    @PatchMapping("/{id}/estado")
    public Cita cambiarEstado(@PathVariable Integer id, @RequestParam String valor) {
        if (!ESTADOS.contains(valor)) throw new IllegalArgumentException("Estado inválido. Use: " + ESTADOS);
        Cita c = find(id);
        verificarAlcance(c);
        if (contexto.obtener().es(Grupos.MEDICO) && !"Atendida".equals(valor) && !"No asistió".equals(valor)) {
            throw new AccesoDenegadoException("El médico solo puede marcar sus citas como Atendida o No asistió");
        }
        c.setEstadoCita(valor);
        validar(c, id);
        return saveAndReload(c);
    }
}
