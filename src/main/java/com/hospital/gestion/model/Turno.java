package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "Turno")
@Getter @Setter
public class Turno {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_turno")
    private Integer idTurno;

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

    @Pattern(regexp = "Programado|Cumplido|Cancelado",
             message = "estadoTurno debe ser: Programado, Cumplido o Cancelado")
    @Column(name = "estado_turno", nullable = false, length = 20)
    private String estadoTurno = "Programado";

    @JsonIgnore
    @AssertTrue(message = "horaInicio y horaFin no pueden ser iguales")
    public boolean isHorasValidas() {
        return horaInicio == null || horaFin == null || !horaInicio.equals(horaFin);
    }
}
