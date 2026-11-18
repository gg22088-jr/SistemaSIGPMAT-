# API REST – Gestión Hospitalaria (Spring Boot + MySQL)

## Requisitos
- Java 17+ y Maven 3.8+
- MySQL 8 con el script SQL ya ejecutado (base `gestion_hospitalaria`)

## Configuración
Edita `src/main/resources/application.properties` o usa variables de entorno `DB_USER` y `DB_PASSWORD`.

## Ejecutar
    mvn spring-boot:run

Swagger UI: http://localhost:8080/swagger-ui.html

## Endpoints (todos con GET lista, GET /{id}, POST, PUT /{id}, DELETE /{id})
| Recurso | Ruta base | Extras |
|---|---|---|
| Usuario | /api/usuarios | POST /login |
| Especialidad | /api/especialidades | |
| Departamento | /api/departamentos | |
| Empleado | /api/empleados | /departamento/{id}, /estado/{estado} |
| Turno | /api/turnos | /empleado/{id}, /departamento/{id}, /fecha/{yyyy-MM-dd}, /rango?desde=&hasta=, PATCH /{id}/estado?valor= |
| Disponibilidad | /api/disponibilidades | /empleado/{id}, /dia/{dia} |
| Vacacion | /api/vacaciones | /empleado/{id}, /estado/{estado}, PATCH /{id}/estado?valor= |
| Permiso | /api/permisos | /empleado/{id}, /estado/{estado}, PATCH /{id}/estado?valor= |
| Incidencia | /api/incidencias | /empleado/{id}, /tipo/{tipo} |
| Reporte | /api/reportes | /usuario/{id}, /tipo/{tipo} |

## Ejemplos JSON
POST /api/usuarios
    { "nombreUsuario": "admin", "contrasena": "Clave123", "rol": "Administrador" }

POST /api/empleados  (las relaciones se envían solo con su id)
    {
      "departamento": { "idDepartamento": 1 },
      "especialidad": { "idEspecialidad": 1 },
      "usuario": { "idUsuario": 1 },
      "nombre": "Ana", "apellido": "López", "documentoId": "01234567-8",
      "telefono": "70001234", "correo": "ana@hospital.com", "cargo": "Enfermera"
    }

POST /api/turnos
    { "empleado": {"idEmpleado": 1}, "departamento": {"idDepartamento": 1},
      "fecha": "2026-11-10", "horaInicio": "07:00:00", "horaFin": "15:00:00" }

POST /api/permisos
    { "empleado": {"idEmpleado": 1}, "tipoPermiso": "Medico",
      "fechaInicio": "2026-11-10T08:00:00", "fechaFin": "2026-11-10T12:00:00", "motivo": "Cita médica" }

## Códigos de error
400 datos inválidos · 401 login incorrecto · 404 no encontrado · 409 duplicado / llave foránea / registro con datos relacionados
