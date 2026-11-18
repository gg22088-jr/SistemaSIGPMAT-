package com.hospital.gestion.util;

import com.hospital.gestion.model.Cita;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

/** Restricciones de filas reutilizables para los controladores. */
public final class Alcance {

    private Alcance() { }

    /** No devuelve ninguna fila. */
    public static <T> Specification<T> ninguno() {
        return (root, query, cb) -> cb.disjunction();
    }

    /** Filas cuyo campo (siguiendo la ruta de atributos) es igual al valor. Si el valor es nulo, no devuelve nada. */
    public static <T> Specification<T> campo(Object valor, String... ruta) {
        return (root, query, cb) -> {
            if (valor == null) return cb.disjunction();
            Path<Object> p = root.get(ruta[0]);
            for (int i = 1; i < ruta.length; i++) p = p.get(ruta[i]);
            return cb.equal(p, valor);
        };
    }

    /** Filas cuyo campo (por ruta) está dentro de la lista de valores. */
    public static <T> Specification<T> valorEn(Collection<String> valores, String... ruta) {
        return (root, query, cb) -> {
            Path<Object> p = root.get(ruta[0]);
            for (int i = 1; i < ruta.length; i++) p = p.get(ruta[i]);
            return p.in(valores);
        };
    }

    /** Pacientes que tienen al menos una cita con ese empleado ("empleado") o en ese departamento ("departamento"). */
    public static <T> Specification<T> pacientesConCita(String relacion, Integer valor) {
        return (root, query, cb) -> {
            if (valor == null) return cb.disjunction();
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Cita> c = sub.from(Cita.class);
            String idAtributo = "empleado".equals(relacion) ? "idEmpleado" : "idDepartamento";
            sub.select(c.<Integer>get("idCita")).where(
                    cb.equal(c.get("paciente").get("idPaciente"), root.get("idPaciente")),
                    cb.equal(c.get(relacion).get(idAtributo), valor));
            return cb.exists(sub);
        };
    }
}
