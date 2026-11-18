package com.hospital.gestion.repository;

import com.hospital.gestion.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Integer>, JpaSpecificationExecutor<Cita> {

    List<Cita> findByPaciente_IdPaciente(Integer idPaciente);
    List<Cita> findByEmpleado_IdEmpleado(Integer idEmpleado);
    List<Cita> findByFecha(LocalDate fecha);
    List<Cita> findByEstadoCita(String estadoCita);

    boolean existsByPaciente_IdPacienteAndEmpleado_IdEmpleado(Integer idPaciente, Integer idEmpleado);
    boolean existsByPaciente_IdPacienteAndDepartamento_IdDepartamento(Integer idPaciente, Integer idDepartamento);

    /** Citas no canceladas entre dos fechas (inclusive). */
    @Query("select count(c) from Cita c where c.fecha between :desde and :hasta and c.estadoCita <> 'Cancelada'")
    long contarEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** ¿El médico ya tiene otra cita programada que se cruza con ese horario? */
    @Query("select count(c) > 0 from Cita c where c.empleado.idEmpleado = :emp and c.fecha = :fecha "
         + "and c.estadoCita = 'Programada' and c.horaInicio < :fin and c.horaFin > :inicio and c.idCita <> :excluir")
    boolean haySolapeMedico(@Param("emp") Integer emp, @Param("fecha") LocalDate fecha,
                            @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin,
                            @Param("excluir") Integer excluir);

    /** ¿El paciente ya tiene otra cita programada que se cruza con ese horario? */
    @Query("select count(c) > 0 from Cita c where c.paciente.idPaciente = :pac and c.fecha = :fecha "
         + "and c.estadoCita = 'Programada' and c.horaInicio < :fin and c.horaFin > :inicio and c.idCita <> :excluir")
    boolean haySolapePaciente(@Param("pac") Integer pac, @Param("fecha") LocalDate fecha,
                              @Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin,
                              @Param("excluir") Integer excluir);
}
