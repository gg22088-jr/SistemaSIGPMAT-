package com.hospital.gestion.repository;

import com.hospital.gestion.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Integer>, JpaSpecificationExecutor<Incidencia> {
    List<Incidencia> findByEmpleado_IdEmpleado(Integer idEmpleado);
    List<Incidencia> findByTipoIncidencia(String tipoIncidencia);

    /** Incidencias reportadas entre dos momentos (desde incluido, hasta excluido). */
    @Query("select i from Incidencia i where i.fechaReporte >= :desde and i.fechaReporte < :hasta")
    List<Incidencia> entre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
