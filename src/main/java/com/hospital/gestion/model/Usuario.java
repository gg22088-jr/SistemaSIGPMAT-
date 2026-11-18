package com.hospital.gestion.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "Usuario")
@Getter @Setter
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @NotBlank @Size(max = 50)
    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    /** Solo se recibe (WRITE_ONLY): nunca se devuelve en las respuestas. Se guarda con BCrypt. */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    @NotBlank
    @Pattern(regexp = "Administrador|Director|Jefe de Área|Personal Médico|Enfermero|Auxiliar de Enfermería|Recepcionista|Secretaria|Contador|Recursos Humanos|Conserje|Motorista|Vigilante|Farmacéutico|Laboratorista|Radiólogo|Técnico de Mantenimiento|Cocinero|Nutricionista|Lavandería|Trabajador Social|Personal de Limpieza|Odontólogo|Fisioterapeuta|Anestesiólogo|Cirujano|Pediatra|Cardiólogo",
             message = "rol no válido: debe ser uno de los roles definidos en la restricción chk_usuario_rol")
    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @Pattern(regexp = "Activo|Inactivo", message = "estado debe ser: Activo o Inactivo")
    @Column(name = "estado", nullable = false, length = 15)
    private String estado = "Activo";
}
