-- C-04 auth-usuarios-roles: catálogo de roles y usuarios del personal. SQL válido en MySQL 8 y en
-- H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway) por la "ó" de "Recepción".
-- Un usuario tiene un solo rol (PA-09). Los usuarios no se borran: se desactivan (activo).
-- El rol usa su código como clave (design D5) para que el CHECK de coherencia rol–profesional pueda
-- vivir en la base sin consultar otra tabla.

CREATE TABLE rol (
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (codigo),
    CONSTRAINT uk_rol_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

-- Datos de referencia fijos: existen en todo entorno, incluido el perfil test (no es un seed de demo).
INSERT INTO rol (codigo, nombre) VALUES
    ('administrador', 'Administrador'),
    ('recepcion', 'Recepción'),
    ('profesional', 'Profesional');

CREATE TABLE usuario (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(254) NOT NULL,
    credencial_hash VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    profesional_id  BIGINT       NULL,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_profesional UNIQUE (profesional_id),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol) REFERENCES rol (codigo),
    CONSTRAINT ck_usuario_rol_profesional CHECK (
        (rol = 'profesional' AND profesional_id IS NOT NULL)
        OR (rol <> 'profesional' AND profesional_id IS NULL)),
    CONSTRAINT fk_usuario_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id)
) ENGINE=InnoDB;
