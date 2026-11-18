package com.hospital.gestion.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "Paciente")
@Getter @Setter
public class Paciente {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Integer idPaciente;

    @NotBlank @Size(max = 20)
    @Column(name = "documento_id", nullable = false, unique = true, length = 20)
    private String documentoId;

    @NotBlank @Size(max = 50)
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @NotBlank @Size(max = 50)
    @Column(name = "apellido", nullable = false, length = 50)
    private String apellido;

    @NotNull @Past(message = "fechaNacimiento debe ser una fecha pasada")
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @NotBlank
    @Pattern(regexp = "Masculino|Femenino|Otro", message = "sexo debe ser: Masculino, Femenino u Otro")
    @Column(name = "sexo", nullable = false, length = 15)
    private String sexo;

    @Size(max = 15)
    @Column(name = "telefono", length = 15)
    private String telefono;

    @Email @Size(max = 100)
    @Column(name = "correo", length = 100)
    private String correo;

    @Size(max = 255)
    @Column(name = "direccion")
    private String direccion;

    /** La asigna la base de datos (DEFAULT CURRENT_TIMESTAMP). */
    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
