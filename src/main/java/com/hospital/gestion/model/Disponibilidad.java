package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "Disponibilidad")
@Getter @Setter
public class Disponibilidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disponi")
    private Integer idDisponi;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_empleado", nullable = false)
    private Empleado empleado;

    @NotBlank
    @Pattern(regexp = "Lunes|Martes|Miércoles|Jueves|Viernes|Sábado|Domingo",
             message = "diaSemana debe ser un día válido en español (con tilde)")
    @Column(name = "dia_semana", nullable = false, length = 15)
    private String diaSemana;

    @NotNull
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @JsonIgnore
    @AssertTrue(message = "horaInicio y horaFin no pueden ser iguales")
    public boolean isHorasValidas() {
        return horaInicio == null || horaFin == null || !horaInicio.equals(horaFin);
    }
}
