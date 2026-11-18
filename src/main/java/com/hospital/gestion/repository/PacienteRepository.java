package com.hospital.gestion.repository;

import com.hospital.gestion.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PacienteRepository extends JpaRepository<Paciente, Integer>, JpaSpecificationExecutor<Paciente> {

    @Query("select p from Paciente p where lower(p.nombre) like lower(concat('%', :q, '%')) "
         + "or lower(p.apellido) like lower(concat('%', :q, '%')) or p.documentoId like concat('%', :q, '%')")
    List<Paciente> buscar(@Param("q") String q);
}
