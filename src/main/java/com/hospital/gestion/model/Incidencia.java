package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Incidencia")
@Getter @Setter
public class Incidencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_incidencia")
    private Integer idIncidencia;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @NotBlank
    @Pattern(regexp = "Inasistencia|Retraso|Incapacidad",
             message = "tipoIncidencia debe ser: Inasistencia, Retraso o Incapacidad")
    @Column(name = "tipo_incidencia", nullable = false, length = 50)
    private String tipoIncidencia;

    /** La asigna la base de datos (DEFAULT CURRENT_TIMESTAMP). */
    @Column(name = "fecha_reporte", insertable = false, updatable = false)
    private LocalDateTime fechaReporte;

    @NotBlank
    @Column(name = "descripcion", nullable = false, columnDefinition = "TEXT")
    private String descripcion;
}
