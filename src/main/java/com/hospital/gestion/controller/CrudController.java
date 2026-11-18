package com.hospital.gestion.controller;

import com.hospital.gestion.exception.AccesoDenegadoException;
import com.hospital.gestion.exception.ResourceNotFoundException;
import com.hospital.gestion.model.Empleado;
import com.hospital.gestion.security.Contexto;
import com.hospital.gestion.security.ContextoActual;
import com.hospital.gestion.security.Grupos;
import com.hospital.gestion.util.Busqueda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CRUD genérico con alcance de datos: GET (lista, con paginación y búsqueda opcionales), GET /{id}, POST,
 * PUT /{id} y DELETE /{id}. El acceso al recurso lo decide Permisos (por URL); el ALCANCE por fila lo decide
 * cada controlador con alcance() (para listar) y enAlcance() (para un registro concreto).
 */
@Transactional
public abstract class CrudController<T, ID> {

    protected final JpaRepository<T, ID> repo;
    protected final EntityManager em;
    private final String nombre;
    private final Class<?> clase;

    @Autowired
    protected ContextoActual contexto;

    protected CrudController(JpaRepository<T, ID> repo, EntityManager em, String nombre) {
        this.repo = repo;
        this.em = em;
        this.nombre = nombre;
        this.clase = tipoEntidad(getClass());
    }

    protected abstract void assignId(T entity, ID id);

    protected void beforeCreate(T entity) { }

    protected void beforeUpdate(ID id, T entity) { }

    /** Filas que el usuario puede ver al listar (null = sin restricción). */
    protected Specification<T> alcance(Contexto ctx) {
        return null;
    }

    /** ¿Este registro concreto está dentro del alcance del usuario? */
    protected boolean enAlcance(Contexto ctx, T entidad) {
        return true;
    }

    protected void verificarAlcance(T entidad) {
        if (!enAlcance(contexto.obtener(), entidad)) {
            throw new AccesoDenegadoException("No tiene acceso a este registro");
        }
    }

    /** Para los endpoints de consulta propios de cada controlador: deja solo lo que el usuario puede ver. */
    protected List<T> filtrar(List<T> lista) {
        Contexto ctx = contexto.obtener();
        return lista.stream().filter(e -> enAlcance(ctx, e)).toList();
    }

    /**
     * Sin page devuelve la lista completa (dentro del alcance del usuario). Con page se pagina:
     *   GET /api/citas?page=0&size=15&sort=fecha,desc&q=texto
     * y responde {contenido, pagina, tamano, totalElementos, totalPaginas}.
     */
    @GetMapping
    public Object listar(@RequestParam(required = false) Integer page,
                         @RequestParam(defaultValue = "15") int size,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String sort) {
        Specification<T> filtro = Specification.where(alcance(contexto.obtener()));
        if (q != null && !q.isBlank()) filtro = filtro.and(Busqueda.<T>texto(q.trim(), em));
        if (page == null) return ejecutor().findAll(filtro);

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("page debe ser 0 o más y size debe estar entre 1 y 100");
        }
        Pageable pageable = PageRequest.of(page, size, orden(sort));
        Page<T> resultado = ejecutor().findAll(filtro, pageable);

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("contenido", resultado.getContent());
        respuesta.put("pagina", resultado.getNumber());
        respuesta.put("tamano", resultado.getSize());
        respuesta.put("totalElementos", resultado.getTotalElements());
        respuesta.put("totalPaginas", resultado.getTotalPages());
        return respuesta;
    }

    @GetMapping("/{id}")
    public T findOne(@PathVariable ID id) {
        T entidad = find(id);
        verificarAlcance(entidad);
        return entidad;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public T create(@Valid @RequestBody T body) {
        assignId(body, null);
        verificarAlcance(body);
        beforeCreate(body);
        return saveAndReload(body);
    }

    @PutMapping("/{id}")
    public T update(@PathVariable ID id, @Valid @RequestBody T body) {
        verificarAlcance(find(id));
        assignId(body, id);
        verificarAlcance(body);
        beforeUpdate(id, body);
        return saveAndReload(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable ID id) {
        verificarAlcance(find(id));
        repo.deleteById(id);
    }

    protected T find(ID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(nombre + " con id " + id + " no encontrado"));
    }

    /** Guarda y vuelve a leer de la BD para devolver las relaciones completas y los valores DEFAULT. */
    @SuppressWarnings("unchecked")
    protected T saveAndReload(T entity) {
        T saved = repo.saveAndFlush(entity);
        ID id = (ID) em.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(saved);
        em.clear();
        return find(id);
    }

    // ---------------- ayudas de alcance ----------------

    /** Departamento del empleado (usa el dato ya cargado o lo consulta si solo viene el id). */
    protected Integer deptoDe(Empleado e) {
        if (e == null) return null;
        if (e.getDepartamento() != null && e.getDepartamento().getIdDepartamento() != null) {
            return e.getDepartamento().getIdDepartamento();
        }
        if (e.getIdEmpleado() == null) return null;
        List<Integer> r = em.createQuery("select e.departamento.idDepartamento from Empleado e where e.idEmpleado = :id", Integer.class)
                .setParameter("id", e.getIdEmpleado()).getResultList();
        return r.isEmpty() ? null : r.get(0);
    }

    /** ¿El empleado tiene un rol que atiende citas (grupo médico o Jefe de Área)? */
    protected boolean atiendeCitas(Empleado e) {
        if (e == null) return false;
        String rol = e.getUsuario() != null ? e.getUsuario().getRol() : null;
        if (rol == null && e.getIdEmpleado() != null) {
            List<String> r = em.createQuery("select u.rol from Empleado e join e.usuario u where e.idEmpleado = :id", String.class)
                    .setParameter("id", e.getIdEmpleado()).getResultList();
            rol = r.isEmpty() ? null : r.get(0);
        }
        return rol != null && Grupos.rolesQueAtienden().contains(rol);
    }

    /** Nadie resuelve sus propias solicitudes. */
    protected void noEsPropia(Empleado e) {
        Integer miEmpleado = contexto.obtener().idEmpleado();
        if (miEmpleado != null && e != null && miEmpleado.equals(e.getIdEmpleado())) {
            throw new AccesoDenegadoException("No puede resolver sus propias solicitudes");
        }
    }

    // ---------------- apoyo para la paginación ----------------

    @SuppressWarnings("unchecked")
    private JpaSpecificationExecutor<T> ejecutor() {
        if (!(repo instanceof JpaSpecificationExecutor)) {
            throw new IllegalStateException("El repositorio de " + nombre + " no admite búsquedas");
        }
        return (JpaSpecificationExecutor<T>) repo;
    }

    /** sort = campo o campo,asc|desc. Sin sort: los más recientes primero (id descendente). */
    @SuppressWarnings("unchecked")
    private Sort orden(String sort) {
        EntityType<T> tipo = em.getMetamodel().entity((Class<T>) clase);
        Set<String> campos = new HashSet<>();
        String idNombre = null;
        for (SingularAttribute<? super T, ?> a : tipo.getSingularAttributes()) {
            campos.add(a.getName());
            if (a.isId()) idNombre = a.getName();
        }
        if (sort == null || sort.isBlank()) {
            return idNombre == null ? Sort.unsorted() : Sort.by(Sort.Direction.DESC, idNombre);
        }
        String[] partes = sort.split(",");
        String campo = partes[0].trim();
        if (!campos.contains(campo)) throw new IllegalArgumentException("Campo de orden inválido: " + campo);
        boolean descendente = partes.length > 1 && "desc".equalsIgnoreCase(partes[1].trim());
        return Sort.by(descendente ? Sort.Direction.DESC : Sort.Direction.ASC, campo);
    }

    /** Clase de la entidad (primer parámetro genérico de CrudController), aunque el bean esté envuelto en un proxy. */
    private static Class<?> tipoEntidad(Class<?> c) {
        Class<?> actual = c;
        while (actual != null && actual.getSuperclass() != CrudController.class) {
            actual = actual.getSuperclass();
        }
        if (actual == null) throw new IllegalStateException("No se pudo determinar la entidad de " + c.getName());
        return (Class<?>) ((ParameterizedType) actual.getGenericSuperclass()).getActualTypeArguments()[0];
    }
}
