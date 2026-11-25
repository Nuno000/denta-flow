-- C-03 modelo-recursos-consultorio: profesionales, sillones, prestaciones y la relación
-- profesional-prestación. SQL válido en MySQL 8 y en H2 MODE=MySQL.
-- Los recursos no se borran: se desactivan (activo/activa). Las restricciones tienen nombre propio
-- y único en todo el esquema porque H2 no permite repetirlos entre tablas.

CREATE TABLE profesional (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(100) NOT NULL,
    apellido   VARCHAR(100) NOT NULL,
    matricula  VARCHAR(30)  NULL,
    email      VARCHAR(254) NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_profesional_email UNIQUE (email),
    CONSTRAINT uk_profesional_matricula UNIQUE (matricula)
) ENGINE=InnoDB;

CREATE TABLE sillon (
    id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    activo BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_sillon_nombre UNIQUE (nombre)
) ENGINE=InnoDB;

CREATE TABLE prestacion (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre           VARCHAR(100) NOT NULL,
    duracion_minutos INT          NOT NULL,
    activa           BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_prestacion_nombre UNIQUE (nombre),
    CONSTRAINT ck_prestacion_duracion_positiva CHECK (duracion_minutos > 0)
) ENGINE=InnoDB;

CREATE TABLE profesional_prestacion (
    profesional_id BIGINT NOT NULL,
    prestacion_id  BIGINT NOT NULL,
    CONSTRAINT pk_profesional_prestacion PRIMARY KEY (profesional_id, prestacion_id),
    CONSTRAINT fk_profesional_prestacion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT fk_profesional_prestacion_prestacion FOREIGN KEY (prestacion_id) REFERENCES prestacion (id)
) ENGINE=InnoDB;

CREATE INDEX idx_profesional_prestacion_prestacion ON profesional_prestacion (prestacion_id);
