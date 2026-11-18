package com.hospital.gestion.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Búsqueda de texto para cualquier entidad: compara contra sus campos de texto y contra los
 * campos de texto de los registros relacionados (por ejemplo, el nombre del empleado en un turno).
 */
public final class Busqueda {

    private static final Set<String> EXCLUIDOS = Set.of("contrasena");

    private Busqueda() { }

    public static <T> Specification<T> texto(String q, EntityManager em) {
        final String patron = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            for (SingularAttribute<? super T, ?> a : root.getModel().getSingularAttributes()) {
                if (a.getJavaType() == String.class) {
                    if (!EXCLUIDOS.contains(a.getName())) {
                        condiciones.add(cb.like(cb.lower(root.<String>get(a.getName())), patron));
                    }
                } else if (a.isAssociation()) {
                    Join<T, Object> relacion = root.join(a.getName(), JoinType.LEFT);
                    for (SingularAttribute<?, ?> b : em.getMetamodel().entity(a.getJavaType()).getSingularAttributes()) {
                        if (b.getJavaType() == String.class && !EXCLUIDOS.contains(b.getName())) {
                            condiciones.add(cb.like(cb.lower(relacion.<String>get(b.getName())), patron));
                        }
                    }
                }
            }
            return cb.or(condiciones.toArray(new Predicate[0]));
        };
    }
}
