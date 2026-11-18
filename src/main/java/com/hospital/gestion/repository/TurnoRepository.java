package com.hospital.gestion.repository;

import com.hospital.gestion.model.Turno;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TurnoRepository extends JpaRepository<Turno, Integer>, JpaSpecificationExecutor<Turno> {
    List<Turno> findByEmpleado_IdEmpleado(Integer idEmpleado);
    List<Turno> findByDepartamento_IdDepartamento(Integer idDepartamento);
    List<Turno> findByFecha(LocalDate fecha);
    List<Turno> findByFechaBetween(LocalDate desde, LocalDate hasta);

    /** Turnos no cancelados del empleado en un rango de fechas, sin contar el turno indicado. */
    @Query("select t from Turno t where t.empleado.idEmpleado = :emp and t.fecha between :desde and :hasta "
         + "and t.estadoTurno <> 'Cancelado' and t.idTurno <> :excluir")
    List<Turno> turnosCercanos(@Param("emp") Integer emp, @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta, @Param("excluir") Integer excluir);
}
