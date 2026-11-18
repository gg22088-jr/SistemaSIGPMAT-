package com.hospital.gestion.repository;

import com.hospital.gestion.model.Vacacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface VacacionRepository extends JpaRepository<Vacacion, Integer>, JpaSpecificationExecutor<Vacacion> {
    List<Vacacion> findByEmpleado_IdEmpleado(Integer idEmpleado);
    List<Vacacion> findByEstadoVac(String estadoVac);

    /** ¿Tiene vacaciones aprobadas que toquen alguno de los días entre desde y hasta? */
    @Query("select count(v) > 0 from Vacacion v where v.empleado.idEmpleado = :emp and v.estadoVac = 'Aprobado' "
         + "and v.fechaInicio <= :hasta and v.fechaFin >= :desde")
    boolean hayVacacionAprobada(@Param("emp") Integer emp, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
