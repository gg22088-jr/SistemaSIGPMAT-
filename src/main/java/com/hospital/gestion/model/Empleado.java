package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Empleado")
@Getter @Setter
public class Empleado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empleado")
    private Integer idEmpleado;

    /** Opcional: un empleado puede no tener usuario del sistema. Enviar {"idUsuario": 1}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_departamento", nullable = false)
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_especialidad")
    private Especialidad especialidad;

    @NotBlank @Size(max = 50)
    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @NotBlank @Size(max = 50)
    @Column(name = "apellido", nullable = false, length = 50)
    private String apellido;

    @NotBlank @Size(max = 20)
    @Column(name = "documento_id", nullable = false, unique = true, length = 20)
    private String documentoId;

    @Size(max = 15)
    @Column(name = "telefono", length = 15)
    private String telefono;

    @NotBlank @Email @Size(max = 100)
    @Column(name = "correo", nullable = false, unique = true, length = 100)
    private String correo;

    @NotBlank @Size(max = 50)
    @Column(name = "cargo", nullable = false, length = 50)
    private String cargo;

    @Pattern(regexp = "Activo|Vacaciones|Incapacitado",
             message = "estadoLaboral debe ser: Activo, Vacaciones o Incapacitado")
    @Column(name = "estado_laboral", nullable = false, length = 20)
    private String estadoLaboral = "Activo";
}
