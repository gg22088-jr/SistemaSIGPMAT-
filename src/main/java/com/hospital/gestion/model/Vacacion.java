package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "Vacacion")
@Getter @Setter
public class Vacacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vacacion")
    private Integer idVacacion;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @NotNull
    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Pattern(regexp = "Solicitado|Aprobado|Rechazado",
             message = "estadoVac debe ser: Solicitado, Aprobado o Rechazado")
    @Column(name = "estado_vac", nullable = false, length = 20)
    private String estadoVac = "Solicitado";

    @JsonIgnore
    @AssertTrue(message = "fechaFin no puede ser anterior a fechaInicio")
    public boolean isFechasValidas() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
