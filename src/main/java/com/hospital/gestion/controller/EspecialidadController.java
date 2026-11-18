package com.hospital.gestion.controller;

import com.hospital.gestion.model.Especialidad;
import com.hospital.gestion.repository.EspecialidadRepository;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/especialidades")
public class EspecialidadController extends CrudController<Especialidad, Integer> {

    public EspecialidadController(EspecialidadRepository repo, EntityManager em) {
        super(repo, em, "Especialidad");
    }

    @Override
    protected void assignId(Especialidad e, Integer id) {
        e.setIdEspecialidad(id);
    }
}
