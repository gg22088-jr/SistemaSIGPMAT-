package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Especialidad")
@Getter @Setter
public class Especialidad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_especialidad")
    private Integer idEspecialidad;

    @NotBlank @Size(max = 100)
    @Column(name = "nombre_especialidad", nullable = false, unique = true, length = 100)
    private String nombreEspecialidad;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;
}
