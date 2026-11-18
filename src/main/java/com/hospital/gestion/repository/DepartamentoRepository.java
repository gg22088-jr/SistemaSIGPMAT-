package com.hospital.gestion.repository;

import com.hospital.gestion.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DepartamentoRepository extends JpaRepository<Departamento, Integer>, JpaSpecificationExecutor<Departamento> {

}
