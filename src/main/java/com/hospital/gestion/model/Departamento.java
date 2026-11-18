package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Departamento")
@Getter @Setter
public class Departamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departamento")
    private Integer idDepartamento;

    @NotBlank @Size(max = 100)
    @Column(name = "nombre_dept", nullable = false, unique = true, length = 100)
    private String nombreDept;

    @Column(name = "descripcion", columnDefinition = "TEXT")
    private String descripcion;

    @NotNull @Min(0)
    @Column(name = "cap_cobertura", nullable = false)
    private Integer capCobertura;
}
