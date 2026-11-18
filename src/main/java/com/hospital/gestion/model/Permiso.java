package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Permiso")
@Getter @Setter
public class Permiso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Integer idPermiso;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @NotBlank
    @Pattern(regexp = "Medico|Personal|Duelo", message = "tipoPermiso debe ser: Medico, Personal o Duelo")
    @Column(name = "tipo_permiso", nullable = false, length = 50)
    private String tipoPermiso;

    @NotNull
    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @NotNull
    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @NotBlank
    @Column(name = "motivo", nullable = false, columnDefinition = "TEXT")
    private String motivo;

    @Pattern(regexp = "Pendiente|Aprobado|Rechazado",
             message = "estadoPer debe ser: Pendiente, Aprobado o Rechazado")
    @Column(name = "estado_per", nullable = false, length = 20)
    private String estadoPer = "Pendiente";

    @JsonIgnore
    @AssertTrue(message = "fechaFin no puede ser anterior a fechaInicio")
    public boolean isFechasValidas() {
        return fechaInicio == null || fechaFin == null || !fechaFin.isBefore(fechaInicio);
    }
}
