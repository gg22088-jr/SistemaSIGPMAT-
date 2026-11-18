package com.hospital.gestion.repository;

import com.hospital.gestion.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByNombreUsuario(String nombreUsuario);

    long countByEstado(String estado);

    @Query("select count(u) from Usuario u where not exists (select e.idEmpleado from Empleado e where e.usuario = u)")
    long contarSinEmpleado();
}
