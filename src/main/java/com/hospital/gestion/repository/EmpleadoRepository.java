package com.hospital.gestion.repository;

import com.hospital.gestion.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository extends JpaRepository<Empleado, Integer>, JpaSpecificationExecutor<Empleado> {
    List<Empleado> findByDepartamento_IdDepartamento(Integer idDepartamento);
    List<Empleado> findByEstadoLaboral(String estadoLaboral);
    Optional<Empleado> findFirstByUsuario_IdUsuario(Integer idUsuario);
}
