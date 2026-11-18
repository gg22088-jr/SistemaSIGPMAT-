package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Cita")
@Getter @Setter
public class Cita {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Integer idCita;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    /** Médico que atiende la cita. */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    @NotNull
    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @NotNull
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @NotBlank
    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Pattern(regexp = "Programada|Atendida|Cancelada|No asistió",
             message = "estadoCita debe ser: Programada, Atendida, Cancelada o No asistió")
    @Column(name = "estado_cita", nullable = false, length = 20)
    private String estadoCita = "Programada";

    @JsonIgnore
    @AssertTrue(message = "horaFin debe ser posterior a horaInicio")
    public boolean isHorasValidas() {
        return horaInicio == null || horaFin == null || horaFin.isAfter(horaInicio);
    }
}
