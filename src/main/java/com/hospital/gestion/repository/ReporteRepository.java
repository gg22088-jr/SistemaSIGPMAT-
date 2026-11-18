package com.hospital.gestion.repository;

import com.hospital.gestion.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReporteRepository extends JpaRepository<Reporte, Integer>, JpaSpecificationExecutor<Reporte> {
    List<Reporte> findByUsuario_IdUsuario(Integer idUsuario);
    List<Reporte> findByTipoReporte(String tipoReporte);
}
