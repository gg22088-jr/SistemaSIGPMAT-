-- ============================================
-- BASE DE DATOS: GESTION HOSPITALARIA (SIGPMAT)
-- MOTOR: MYSQL 8.0.16+
-- Script completo: estructura + datos de prueba 
-- finalllllll
-- ============================================
-- Para volver a crear todo desde cero, descomenta la siguiente linea
-- (BORRA la base de datos completa):
-- DROP DATABASE IF EXISTS gestion_hospitalaria;
CREATE DATABASE IF NOT EXISTS gestion_hospitalaria
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
USE gestion_hospitalaria;
-- ============================================
-- 1. TABLA: USUARIO
-- ============================================
CREATE TABLE Usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    contrasena VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    estado VARCHAR(15) NOT NULL DEFAULT 'Activo',
    CONSTRAINT chk_usuario_rol
        CHECK (rol IN (
            'Administrador',
            'Director',
            'Jefe de Área',
            'Personal Médico',
            'Enfermero',
            'Auxiliar de Enfermería',
            'Recepcionista',
            'Secretaria',
            'Contador',
            'Recursos Humanos',
            'Conserje',
            'Motorista',
            'Vigilante',
            'Farmacéutico',
            'Laboratorista',
            'Radiólogo',
            'Técnico de Mantenimiento',
            'Cocinero',
            'Nutricionista',
            'Lavandería',
            'Trabajador Social',
            'Personal de Limpieza',
            'Odontólogo',
            'Fisioterapeuta',
            'Anestesiólogo',
            'Cirujano',
            'Pediatra',
            'Cardiólogo'
        )),

    CONSTRAINT chk_usuario_estado
        CHECK (estado IN ('Activo', 'Inactivo'))
) ENGINE=InnoDB;
-- ============================================
-- 2. TABLA: ESPECIALIDAD
-- ============================================
CREATE TABLE Especialidad (
    id_especialidad INT AUTO_INCREMENT PRIMARY KEY,
    nombre_especialidad VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT
) ENGINE=InnoDB;
-- ============================================
-- 3. TABLA: DEPARTAMENTO
-- ============================================
CREATE TABLE Departamento (
    id_departamento INT AUTO_INCREMENT PRIMARY KEY,
    nombre_dept VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    cap_cobertura INT NOT NULL,

    CONSTRAINT chk_cap_cobertura
        CHECK (cap_cobertura >= 0)
) ENGINE=InnoDB;
-- ============================================
-- 4. TABLA: EMPLEADO
-- ============================================

CREATE TABLE Empleado (
    id_empleado INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT UNIQUE,
    id_departamento INT NOT NULL,
    id_especialidad INT,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    documento_id VARCHAR(20) NOT NULL UNIQUE,
    telefono VARCHAR(15),
    correo VARCHAR(100) NOT NULL UNIQUE,
    cargo VARCHAR(50) NOT NULL,
    estado_laboral VARCHAR(20) NOT NULL DEFAULT 'Activo',

    CONSTRAINT chk_estado_laboral
        CHECK (estado_laboral IN (
            'Activo',
            'Vacaciones',
            'Incapacitado'
        )),

    CONSTRAINT fk_empleado_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES Usuario(id_usuario)
        ON DELETE SET NULL
        ON UPDATE CASCADE,

    CONSTRAINT fk_empleado_departamento
        FOREIGN KEY (id_departamento)
        REFERENCES Departamento(id_departamento)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_empleado_especialidad
        FOREIGN KEY (id_especialidad)
        REFERENCES Especialidad(id_especialidad)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 5. TABLA: TURNO
-- ============================================

CREATE TABLE Turno (
    id_turno INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado INT NOT NULL,
    id_departamento INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    estado_turno VARCHAR(20) NOT NULL DEFAULT 'Programado',

    CONSTRAINT chk_estado_turno
        CHECK (estado_turno IN (
            'Programado',
            'Cumplido',
            'Cancelado'
        )),

    CONSTRAINT chk_horas_turno
        CHECK (hora_inicio <> hora_fin),

    CONSTRAINT fk_turno_empleado
        FOREIGN KEY (id_empleado)
        REFERENCES Empleado(id_empleado)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_turno_departamento
        FOREIGN KEY (id_departamento)
        REFERENCES Departamento(id_departamento)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 6. TABLA: DISPONIBILIDAD
-- ============================================

CREATE TABLE Disponibilidad (
    id_disponi INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado INT NOT NULL,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,

    CONSTRAINT chk_dia_semana
        CHECK (dia_semana IN (
            'Lunes',
            'Martes',
            'Miércoles',
            'Jueves',
            'Viernes',
            'Sábado',
            'Domingo'
        )),

    CONSTRAINT chk_horas_disponibilidad
        CHECK (hora_inicio <> hora_fin),

    CONSTRAINT fk_disponibilidad_empleado
        FOREIGN KEY (id_empleado)
        REFERENCES Empleado(id_empleado)
        ON DELETE CASCADE
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 7. TABLA: VACACION
-- ============================================

CREATE TABLE Vacacion (
    id_vacacion INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado INT NOT NULL,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    estado_vac VARCHAR(20) NOT NULL DEFAULT 'Solicitado',

    CONSTRAINT chk_estado_vacacion
        CHECK (estado_vac IN (
            'Solicitado',
            'Aprobado',
            'Rechazado'
        )),

    CONSTRAINT chk_fechas_vacacion
        CHECK (fecha_fin >= fecha_inicio),

    CONSTRAINT fk_vacacion_empleado
        FOREIGN KEY (id_empleado)
        REFERENCES Empleado(id_empleado)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 8. TABLA: PERMISO
-- ============================================

CREATE TABLE Permiso (
    id_permiso INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado INT NOT NULL,
    tipo_permiso VARCHAR(50) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    motivo TEXT NOT NULL,
    estado_per VARCHAR(20) NOT NULL DEFAULT 'Pendiente',

    CONSTRAINT chk_tipo_permiso
        CHECK (tipo_permiso IN (
            'Medico',
            'Personal',
            'Duelo'
        )),

    CONSTRAINT chk_estado_permiso
        CHECK (estado_per IN (
            'Pendiente',
            'Aprobado',
            'Rechazado'
        )),

    CONSTRAINT chk_fechas_permiso
        CHECK (fecha_fin >= fecha_inicio),

    CONSTRAINT fk_permiso_empleado
        FOREIGN KEY (id_empleado)
        REFERENCES Empleado(id_empleado)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 9. TABLA: INCIDENCIA
-- ============================================

CREATE TABLE Incidencia (
    id_incidencia INT AUTO_INCREMENT PRIMARY KEY,
    id_empleado INT NOT NULL,
    tipo_incidencia VARCHAR(50) NOT NULL,
    fecha_reporte TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion TEXT NOT NULL,

    CONSTRAINT chk_tipo_incidencia
        CHECK (tipo_incidencia IN (
            'Inasistencia',
            'Retraso',
            'Incapacidad'
        )),

    CONSTRAINT fk_incidencia_empleado
        FOREIGN KEY (id_empleado)
        REFERENCES Empleado(id_empleado)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 10. TABLA: REPORTE
-- ============================================

CREATE TABLE Reporte (
    id_reporte INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario INT NOT NULL,
    tipo_reporte VARCHAR(50) NOT NULL,
    fecha_generada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parametros VARCHAR(255),

    CONSTRAINT chk_tipo_reporte
        CHECK (tipo_reporte IN (
            'Asistencia',
            'Cobertura',
            'Horas Extras'
        )),

    CONSTRAINT fk_reporte_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES Usuario(id_usuario)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
) ENGINE=InnoDB;


-- ============================================
-- 11. TABLA: PACIENTE
-- ============================================

CREATE TABLE Paciente (
    id_paciente INT AUTO_INCREMENT PRIMARY KEY,
    documento_id VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(50) NOT NULL,
    apellido VARCHAR(50) NOT NULL,
    fecha_nacimiento DATE NOT NULL,
    sexo VARCHAR(15) NOT NULL,
    telefono VARCHAR(15),
    correo VARCHAR(100),
    direccion VARCHAR(255),
    fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_paciente_sexo
        CHECK (sexo IN ('Masculino', 'Femenino', 'Otro'))
) ENGINE=InnoDB;


-- ============================================
-- 12. TABLA: CITA
-- ============================================

CREATE TABLE Cita (
    id_cita INT AUTO_INCREMENT PRIMARY KEY,
    id_paciente INT NOT NULL,
    id_empleado INT NOT NULL,          -- médico que atiende
    id_departamento INT NOT NULL,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    motivo TEXT NOT NULL,
    estado_cita VARCHAR(20) NOT NULL DEFAULT 'Programada',

    CONSTRAINT chk_estado_cita
        CHECK (estado_cita IN ('Programada', 'Atendida', 'Cancelada', 'No asistió')),

    CONSTRAINT chk_horas_cita
        CHECK (hora_fin > hora_inicio),

    CONSTRAINT fk_cita_paciente
        FOREIGN KEY (id_paciente) REFERENCES Paciente(id_paciente)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_cita_empleado
        FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    CONSTRAINT fk_cita_departamento
        FOREIGN KEY (id_departamento) REFERENCES Departamento(id_departamento)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_cita_empleado_fecha ON Cita(id_empleado, fecha);
CREATE INDEX idx_cita_paciente_fecha ON Cita(id_paciente, fecha);


-- ============================================
-- VERIFICAR LAS TABLAS CREADAS
-- ============================================

SHOW TABLES;


-- ============================================
-- DATOS DE PRUEBA
-- ============================================

-- Hash BCrypt valido de la contrasena de desarrollo: Clave123
-- (cambiala antes de usar el sistema con datos reales)
SET @hash = '$2b$10$6VMFMVkM02bSeyxTDXuDL.ZEt9x35ZC.uCwDzjsE5nKCZGAZU1c0y';

INSERT INTO Especialidad (nombre_especialidad, descripcion) VALUES
('Medicina General', 'Atención médica primaria y prevención de enfermedades.'),
('Pediatría', 'Atención médica especializada en niños y adolescentes.'),
('Cardiología', 'Diagnóstico y tratamiento de enfermedades del corazón.'),
('Dermatología', 'Tratamiento de enfermedades de la piel, cabello y uñas.'),
('Ginecología', 'Atención médica del sistema reproductor femenino.'),
('Neurología', 'Diagnóstico y tratamiento de enfermedades del sistema nervioso.'),
('Oftalmología', 'Prevención y tratamiento de enfermedades de los ojos.'),
('Odontología', 'Prevención y tratamiento de enfermedades bucales.'),
('Traumatología', 'Tratamiento de lesiones y enfermedades del sistema musculoesquelético.'),
('Psiquiatría', 'Diagnóstico y tratamiento de trastornos mentales.'),
('Urología', 'Atención de enfermedades del sistema urinario.'),
('Oncología', 'Diagnóstico y tratamiento del cáncer.'),
('Endocrinología', 'Tratamiento de enfermedades hormonales y metabólicas.'),
('Gastroenterología', 'Atención de enfermedades del aparato digestivo.'),
('Otorinolaringología', 'Tratamiento de enfermedades del oído, nariz y garganta.');

INSERT INTO Usuario (id_usuario, nombre_usuario, contrasena, rol, estado) VALUES
(1, 'admin01', @hash, 'Administrador', 'Activo'),
(2, 'jefe01', @hash, 'Jefe de Área', 'Activo'),
(3, 'medico01', @hash, 'Personal Médico', 'Activo'),
(4, 'medico02', @hash, 'Personal Médico', 'Activo'),
(5, 'jefe02', @hash, 'Jefe de Área', 'Activo'),
(6, 'medico03', @hash, 'Personal Médico', 'Activo'),
(7, 'medico04', @hash, 'Personal Médico', 'Activo'),
(8, 'jefe03', @hash, 'Jefe de Área', 'Activo'),
(9, 'medico05', @hash, 'Personal Médico', 'Inactivo'),
(10, 'medico06', @hash, 'Personal Médico', 'Activo');

INSERT INTO Usuario
(nombre_usuario, contrasena, rol, estado) VALUES
('director01', @hash, 'Director', 'Activo'),
('enfermero01', @hash, 'Enfermero', 'Activo'),
('enfermero02', @hash, 'Enfermero', 'Activo'),
('auxiliar01', @hash, 'Auxiliar de Enfermería', 'Activo'),
('recepcion01', @hash, 'Recepcionista', 'Activo'),
('recepcion02', @hash, 'Recepcionista', 'Activo'),
('secretaria01', @hash, 'Secretaria', 'Activo'),
('contador01', @hash, 'Contador', 'Activo'),
('rrhh01', @hash, 'Recursos Humanos', 'Activo'),
('conserje01', @hash, 'Conserje', 'Activo'),

('conserje02', @hash, 'Conserje', 'Activo'),
('motorista01', @hash, 'Motorista', 'Activo'),
('motorista02', @hash, 'Motorista', 'Activo'),
('vigilante01', @hash, 'Vigilante', 'Activo'),
('vigilante02', @hash, 'Vigilante', 'Activo'),
('farmaceutico01', @hash, 'Farmacéutico', 'Activo'),
('laboratorista01', @hash, 'Laboratorista', 'Activo'),
('radiologo01', @hash, 'Radiólogo', 'Activo'),
('mantenimiento01', @hash, 'Técnico de Mantenimiento', 'Activo'),
('cocinero01', @hash, 'Cocinero', 'Activo'),

('nutricionista01', @hash, 'Nutricionista', 'Activo'),
('lavanderia01', @hash, 'Lavandería', 'Activo'),
('social01', @hash, 'Trabajador Social', 'Activo'),
('limpieza01', @hash, 'Personal de Limpieza', 'Activo'),
('odontologo01', @hash, 'Odontólogo', 'Activo'),
('fisioterapeuta01', @hash, 'Fisioterapeuta', 'Activo'),
('anestesiologo01', @hash, 'Anestesiólogo', 'Activo'),
('cirujano01', @hash, 'Cirujano', 'Activo'),
('pediatra01', @hash, 'Pediatra', 'Activo'),
('cardiologo01', @hash, 'Cardiólogo', 'Activo');

INSERT INTO Departamento
(id_departamento, nombre_dept, descripcion, cap_cobertura) VALUES
(1, 'Emergencias', 'Atención de pacientes en situaciones de emergencia.', 30),
(2, 'Pediatría', 'Atención médica infantil.', 20),
(3, 'Cardiología', 'Atención de enfermedades cardiovasculares.', 15),
(4, 'Cirugía', 'Procedimientos quirúrgicos.', 25),
(5, 'Ginecología', 'Atención de salud femenina.', 15),
(6, 'Laboratorio', 'Análisis clínicos y pruebas médicas.', 12),
(7, 'Radiología', 'Diagnóstico mediante imágenes médicas.', 10),
(8, 'Medicina General', 'Consulta y atención médica general.', 25),
(9, 'Farmacia', 'Control y despacho de medicamentos.', 10),
(10, 'Hospitalización', 'Atención de pacientes ingresados.', 40);

INSERT INTO Empleado
(id_empleado, id_usuario, id_departamento, id_especialidad,
nombre, apellido, documento_id, telefono, correo, cargo, estado_laboral) VALUES
(1, 1, 1, 1, 'Carlos', 'Martínez', 'DOC001', '70010001', 'carlos@hospital.com', 'Administrador', 'Activo'),
(2, 2, 2, 2, 'Ana', 'López', 'DOC002', '70010002', 'ana@hospital.com', 'Jefa de Pediatría', 'Activo'),
(3, 3, 3, 3, 'Roberto', 'García', 'DOC003', '70010003', 'roberto@hospital.com', 'Cardiólogo', 'Activo'),
(4, 4, 4, 4, 'María', 'Hernández', 'DOC004', '70010004', 'maria@hospital.com', 'Cirujana', 'Activo'),
(5, 5, 5, 5, 'José', 'Ramírez', 'DOC005', '70010005', 'jose@hospital.com', 'Jefe de Ginecología', 'Activo'),
(6, 6, 6, 6, 'Laura', 'Castro', 'DOC006', '70010006', 'laura@hospital.com', 'Analista Clínico', 'Activo'),
(7, 7, 7, 7, 'Miguel', 'Flores', 'DOC007', '70010007', 'miguel@hospital.com', 'Radiólogo', 'Vacaciones'),
(8, 8, 8, 8, 'Sofía', 'Morales', 'DOC008', '70010008', 'sofia@hospital.com', 'Jefa de Medicina General', 'Activo'),
(9, 9, 9, 9, 'Pedro', 'Díaz', 'DOC009', '70010009', 'pedro@hospital.com', 'Farmacéutico', 'Incapacitado'),
(10, 10, 10, 10, 'Gabriela', 'Mendoza', 'DOC010', '70010010', 'gabriela@hospital.com', 'Médica Hospitalaria', 'Activo');

INSERT INTO Turno
(id_turno, id_empleado, id_departamento, fecha,
hora_inicio, hora_fin, estado_turno) VALUES
(1, 1, 1, '2026-11-05', '06:00:00', '14:00:00', 'Programado'),
(2, 2, 2, '2026-11-05', '07:00:00', '15:00:00', 'Programado'),
(3, 3, 3, '2026-11-05', '08:00:00', '16:00:00', 'Cumplido'),
(4, 4, 4, '2026-11-06', '06:00:00', '14:00:00', 'Programado'),
(5, 5, 5, '2026-11-06', '14:00:00', '22:00:00', 'Programado'),
(6, 6, 6, '2026-11-07', '07:00:00', '15:00:00', 'Cumplido'),
(7, 7, 7, '2026-11-07', '08:00:00', '16:00:00', 'Cancelado'),
(8, 8, 8, '2026-11-08', '06:00:00', '14:00:00', 'Programado'),
(9, 9, 9, '2026-11-08', '14:00:00', '22:00:00', 'Programado'),
(10, 10, 10, '2026-11-09', '22:00:00', '06:00:00', 'Programado');

INSERT INTO Disponibilidad
(id_disponi, id_empleado, dia_semana, hora_inicio, hora_fin) VALUES
(1, 1, 'Lunes', '06:00:00', '14:00:00'),
(2, 2, 'Martes', '07:00:00', '15:00:00'),
(3, 3, 'Miércoles', '08:00:00', '16:00:00'),
(4, 4, 'Jueves', '06:00:00', '14:00:00'),
(5, 5, 'Viernes', '14:00:00', '22:00:00'),
(6, 6, 'Sábado', '07:00:00', '15:00:00'),
(7, 7, 'Domingo', '08:00:00', '16:00:00'),
(8, 8, 'Lunes', '14:00:00', '22:00:00'),
(9, 9, 'Martes', '06:00:00', '14:00:00'),
(10, 10, 'Miércoles', '22:00:00', '06:00:00');

INSERT INTO Vacacion
(id_vacacion, id_empleado, fecha_inicio, fecha_fin, estado_vac) VALUES
(1, 1, '2026-12-01', '2026-12-10', 'Aprobado'),
(2, 2, '2026-12-05', '2026-12-15', 'Solicitado'),
(3, 3, '2026-11-15', '2026-11-20', 'Aprobado'),
(4, 4, '2026-12-10', '2026-12-20', 'Rechazado'),
(5, 5, '2027-01-05', '2027-01-15', 'Solicitado'),
(6, 6, '2027-02-01', '2027-02-10', 'Aprobado'),
(7, 7, '2026-11-10', '2026-11-20', 'Aprobado'),
(8, 8, '2027-03-01', '2027-03-12', 'Solicitado'),
(9, 9, '2026-12-20', '2026-12-30', 'Rechazado'),
(10, 10, '2027-04-01', '2027-04-10', 'Aprobado');

INSERT INTO Permiso
(id_permiso, id_empleado, tipo_permiso, fecha_inicio,
fecha_fin, motivo, estado_per) VALUES
(1, 1, 'Personal', '2026-11-05 08:00:00', '2026-11-05 12:00:00', 'Trámites personales.', 'Aprobado'),
(2, 2, 'Medico', '2026-11-06 09:00:00', '2026-11-06 13:00:00', 'Consulta médica.', 'Pendiente'),
(3, 3, 'Personal', '2026-11-07 08:00:00', '2026-11-07 10:00:00', 'Asunto familiar.', 'Aprobado'),
(4, 4, 'Duelo', '2026-11-08 06:00:00', '2026-11-09 18:00:00', 'Fallecimiento de familiar.', 'Aprobado'),
(5, 5, 'Medico', '2026-11-10 07:00:00', '2026-11-10 15:00:00', 'Evaluación médica.', 'Rechazado'),
(6, 6, 'Personal', '2026-11-11 08:00:00', '2026-11-11 12:00:00', 'Trámites bancarios.', 'Pendiente'),
(7, 7, 'Medico', '2026-11-12 09:00:00', '2026-11-12 11:00:00', 'Consulta de seguimiento.', 'Aprobado'),
(8, 8, 'Personal', '2026-11-13 08:00:00', '2026-11-13 14:00:00', 'Asunto personal.', 'Aprobado'),
(9, 9, 'Medico', '2026-11-14 07:00:00', '2026-11-14 15:00:00', 'Reposo médico.', 'Pendiente'),
(10, 10, 'Duelo', '2026-11-15 06:00:00', '2026-11-16 18:00:00', 'Fallecimiento de familiar.', 'Aprobado');

INSERT INTO Incidencia
(id_incidencia, id_empleado, tipo_incidencia, fecha_reporte, descripcion) VALUES
(1, 1, 'Retraso', '2026-11-01 07:30:00', 'Llegada tardía al inicio de jornada.'),
(2, 2, 'Inasistencia', '2026-11-02 08:00:00', 'No se presentó a su turno laboral.'),
(3, 3, 'Incapacidad', '2026-11-03 09:00:00', 'Presentó incapacidad médica.'),
(4, 4, 'Retraso', '2026-11-04 07:15:00', 'Llegada tardía de 15 minutos.'),
(5, 5, 'Inasistencia', '2026-11-05 08:00:00', 'Ausencia sin previo aviso.'),
(6, 6, 'Retraso', '2026-11-06 07:20:00', 'Llegada tardía al departamento.'),
(7, 7, 'Incapacidad', '2026-11-07 10:00:00', 'Incapacidad por enfermedad.'),
(8, 8, 'Inasistencia', '2026-11-08 08:00:00', 'Ausencia durante la jornada laboral.'),
(9, 9, 'Retraso', '2026-11-09 07:45:00', 'Llegada tardía de 45 minutos.'),
(10, 10, 'Incapacidad', '2026-11-10 09:30:00', 'Presentó incapacidad médica.');

INSERT INTO Reporte
(id_reporte, id_usuario, tipo_reporte, fecha_generada, parametros) VALUES
(1, 1, 'Asistencia', '2026-11-01 08:00:00', 'Reporte general de asistencia.'),
(2, 2, 'Cobertura', '2026-11-02 09:00:00', 'Cobertura del departamento de Pediatría.'),
(3, 3, 'Horas Extras', '2026-11-03 10:00:00', 'Horas extras del personal médico.'),
(4, 4, 'Asistencia', '2026-11-04 08:30:00', 'Asistencia del personal de cirugía.'),
(5, 5, 'Cobertura', '2026-11-05 11:00:00', 'Cobertura de Ginecología.'),
(6, 6, 'Horas Extras', '2026-11-06 12:00:00', 'Horas extras del laboratorio.'),
(7, 7, 'Asistencia', '2026-11-07 08:00:00', 'Control de asistencia semanal.'),
(8, 8, 'Cobertura', '2026-11-08 09:30:00', 'Cobertura de Medicina General.'),
(9, 9, 'Horas Extras', '2026-11-09 10:00:00', 'Horas extras de Farmacia.'),
(10, 10, 'Asistencia', '2026-11-10 08:00:00', 'Reporte mensual de asistencia.');

INSERT INTO Paciente
(id_paciente, documento_id, nombre, apellido, fecha_nacimiento,
sexo, telefono, correo, direccion) VALUES
(1, 'PAC001', 'Juan', 'Pérez', '1990-05-15', 'Masculino', '78010001', 'juan@gmail.com', 'San Salvador'),
(2, 'PAC002', 'María', 'López', '1985-08-20', 'Femenino', '78010002', 'maria@gmail.com', 'Santa Tecla'),
(3, 'PAC003', 'Carlos', 'Hernández', '2000-03-10', 'Masculino', '78010003', 'carlos@gmail.com', 'Soyapango'),
(4, 'PAC004', 'Ana', 'Martínez', '1995-11-25', 'Femenino', '78010004', 'ana@gmail.com', 'Mejicanos'),
(5, 'PAC005', 'José', 'Ramírez', '1978-02-14', 'Masculino', '78010005', 'jose@gmail.com', 'Antiguo Cuscatlán'),
(6, 'PAC006', 'Sofía', 'Castro', '2015-06-18', 'Femenino', '78010006', 'sofia@gmail.com', 'San Marcos'),
(7, 'PAC007', 'Roberto', 'Flores', '1965-09-30', 'Masculino', '78010007', 'roberto@gmail.com', 'Apopa'),
(8, 'PAC008', 'Gabriela', 'Morales', '1998-12-05', 'Femenino', '78010008', 'gabriela@gmail.com', 'Ilopango'),
(9, 'PAC009', 'Miguel', 'Díaz', '1982-07-22', 'Masculino', '78010009', 'miguel@gmail.com', 'Cuscatancingo'),
(10, 'PAC010', 'Laura', 'Mendoza', '2005-04-12', 'Femenino', '78010010', 'laura@gmail.com', 'San Salvador');

INSERT INTO Cita
(id_cita, id_paciente, id_empleado, id_departamento,
fecha, hora_inicio, hora_fin, motivo, estado_cita) VALUES
(1, 1, 3, 3, '2026-11-10', '08:00:00', '08:30:00', 'Control de presión arterial.', 'Programada'),
(2, 2, 5, 5, '2026-11-10', '09:00:00', '09:30:00', 'Consulta ginecológica.', 'Programada'),
(3, 3, 8, 8, '2026-11-11', '10:00:00', '10:30:00', 'Consulta médica general.', 'Atendida'),
(4, 4, 2, 2, '2026-11-11', '11:00:00', '11:30:00', 'Control pediátrico.', 'Programada'),
(5, 5, 3, 3, '2026-11-12', '08:30:00', '09:00:00', 'Evaluación cardiovascular.', 'Atendida'),
(6, 6, 2, 2, '2026-11-12', '09:30:00', '10:00:00', 'Control de crecimiento infantil.', 'Programada'),
(7, 7, 4, 4, '2026-11-13', '10:00:00', '11:00:00', 'Evaluación para procedimiento quirúrgico.', 'Programada'),
(8, 8, 8, 8, '2026-11-13', '11:30:00', '12:00:00', 'Consulta por dolor de cabeza.', 'No asistió'),
(9, 9, 1, 1, '2026-11-14', '08:00:00', '08:30:00', 'Atención por dolor abdominal.', 'Cancelada'),
(10, 10, 8, 8, '2026-11-14', '09:00:00', '09:30:00', 'Consulta médica preventiva.', 'Programada');


-- ============================================
-- VERIFICACION FINAL (filas por tabla)
-- ============================================

SELECT 'Usuario' AS tabla, COUNT(*) AS filas FROM Usuario
UNION ALL SELECT 'Especialidad', COUNT(*) FROM Especialidad
UNION ALL SELECT 'Departamento', COUNT(*) FROM Departamento
UNION ALL SELECT 'Empleado', COUNT(*) FROM Empleado
UNION ALL SELECT 'Turno', COUNT(*) FROM Turno
UNION ALL SELECT 'Disponibilidad', COUNT(*) FROM Disponibilidad
UNION ALL SELECT 'Vacacion', COUNT(*) FROM Vacacion
UNION ALL SELECT 'Permiso', COUNT(*) FROM Permiso
UNION ALL SELECT 'Incidencia', COUNT(*) FROM Incidencia
UNION ALL SELECT 'Reporte', COUNT(*) FROM Reporte
UNION ALL SELECT 'Paciente', COUNT(*) FROM Paciente
UNION ALL SELECT 'Cita', COUNT(*) FROM Cita;

SELECT nombre_usuario, estado, LENGTH(contrasena) AS largo, LEFT(contrasena, 7) AS inicio
FROM Usuario WHERE nombre_usuario = 'admin01';