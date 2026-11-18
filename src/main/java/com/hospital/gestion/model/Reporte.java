package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "Reporte")
@Getter @Setter
public class Reporte {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotBlank
    @Pattern(regexp = "Asistencia|Cobertura|Horas Extras",
             message = "tipoReporte debe ser: Asistencia, Cobertura u Horas Extras")
    @Column(name = "tipo_reporte", nullable = false, length = 50)
    private String tipoReporte;

    /** La asigna la base de datos (DEFAULT CURRENT_TIMESTAMP). */
    @Column(name = "fecha_generada", insertable = false, updatable = false)
    private LocalDateTime fechaGenerada;

    @Size(max = 255)
    @Column(name = "parametros", length = 255)
    private String parametros;
}
