package com.hospital.gestion.repository;

import com.hospital.gestion.model.Permiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PermisoRepository extends JpaRepository<Permiso, Integer>, JpaSpecificationExecutor<Permiso> {
    List<Permiso> findByEmpleado_IdEmpleado(Integer idEmpleado);
    List<Permiso> findByEstadoPer(String estadoPer);

    /** ¿Tiene un permiso aprobado que se cruce con el intervalo indicado? */
    @Query("select count(p) > 0 from Permiso p where p.empleado.idEmpleado = :emp and p.estadoPer = 'Aprobado' "
         + "and p.fechaInicio < :fin and p.fechaFin > :inicio")
    boolean hayPermisoAprobado(@Param("emp") Integer emp, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
