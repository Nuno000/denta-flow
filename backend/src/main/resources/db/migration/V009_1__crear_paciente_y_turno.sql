-- C-09 modelo-turno-paciente: pacientes y turnos (design D1). SQL válido en MySQL 8 y en H2 MODE=MySQL.
-- Archivo en UTF-8 (default de Flyway). Fechas y horas en UTC (DATETIME(6)).
-- El formato del DNI, teléfono y email se valida en el dominio (sin CHECK con expresiones regulares, por
-- portabilidad); la base garantiza nulos, largo del DNI y unicidad. Sin ON DELETE/UPDATE: MySQL 8 no admite
-- CHECK sobre columnas con acciones referenciales.

CREATE TABLE paciente (
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    dni                          VARCHAR(8)   NOT NULL,
    nombre                       VARCHAR(100) NOT NULL,
    apellido                     VARCHAR(100) NOT NULL,
    telefono                     VARCHAR(16)  NOT NULL,
    email                        VARCHAR(254) NOT NULL,
    consentimiento_privacidad_en DATETIME(6)  NULL,
    creado_en                    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_paciente_dni UNIQUE (dni),
    CONSTRAINT ck_paciente_dni_largo CHECK (CHAR_LENGTH(dni) BETWEEN 7 AND 8)
) ENGINE=InnoDB;

CREATE TABLE turno (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id           BIGINT      NOT NULL,
    profesional_id        BIGINT      NOT NULL,
    sillon_id             BIGINT      NOT NULL,
    prestacion_id         BIGINT      NOT NULL,
    inicio                DATETIME(6) NOT NULL,
    fin                   DATETIME(6) NOT NULL,
    estado                VARCHAR(20) NOT NULL,
    origen                VARCHAR(10) NOT NULL,
    es_sobreturno         BOOLEAN     NOT NULL DEFAULT FALSE,
    creado_por_usuario_id BIGINT      NULL,
    creado_en             DATETIME(6) NOT NULL,
    token_acceso          VARCHAR(64) NOT NULL,
    CONSTRAINT uk_turno_token_acceso UNIQUE (token_acceso),
    CONSTRAINT ck_turno_estado CHECK (estado IN
        ('pendiente', 'confirmado', 'cancelado', 'asistio', 'ausente', 'ausente_con_aviso')),
    CONSTRAINT ck_turno_origen CHECK (origen IN ('online', 'recepcion')),
    CONSTRAINT ck_turno_rango CHECK (fin > inicio),
    CONSTRAINT ck_turno_sobreturno_recepcion CHECK (es_sobreturno = FALSE OR origen = 'recepcion'),
    CONSTRAINT ck_turno_origen_creador CHECK (
        (origen = 'online' AND creado_por_usuario_id IS NULL)
        OR (origen = 'recepcion' AND creado_por_usuario_id IS NOT NULL)),
    CONSTRAINT fk_turno_paciente    FOREIGN KEY (paciente_id)           REFERENCES paciente (id),
    CONSTRAINT fk_turno_profesional FOREIGN KEY (profesional_id)        REFERENCES profesional (id),
    CONSTRAINT fk_turno_sillon      FOREIGN KEY (sillon_id)             REFERENCES sillon (id),
    CONSTRAINT fk_turno_prestacion  FOREIGN KEY (prestacion_id)         REFERENCES prestacion (id),
    CONSTRAINT fk_turno_creado_por  FOREIGN KEY (creado_por_usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

-- Igualdad primero, rango después (ficha C-09 y DD-09). Las FK de profesional y sillón usan estos índices.
CREATE INDEX ix_turno_profesional_solapamiento ON turno (profesional_id, estado, inicio, fin);
CREATE INDEX ix_turno_sillon_solapamiento      ON turno (sillon_id, estado, inicio, fin);
CREATE INDEX ix_turno_paciente                 ON turno (paciente_id);
CREATE INDEX ix_turno_estado_inicio            ON turno (estado, inicio);
CREATE INDEX ix_turno_prestacion               ON turno (prestacion_id);
CREATE INDEX ix_turno_creado_por               ON turno (creado_por_usuario_id);
