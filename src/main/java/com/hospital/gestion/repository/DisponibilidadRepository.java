package com.hospital.gestion.repository;

import com.hospital.gestion.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DisponibilidadRepository extends JpaRepository<Disponibilidad, Integer>, JpaSpecificationExecutor<Disponibilidad> {
    List<Disponibilidad> findByEmpleado_IdEmpleado(Integer idEmpleado);
    List<Disponibilidad> findByDiaSemana(String diaSemana);
}
