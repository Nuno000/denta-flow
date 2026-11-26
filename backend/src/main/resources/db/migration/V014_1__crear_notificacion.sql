-- C-14 email-transaccional-y-enlaces: registro de los emails transaccionales de cada turno (design D2).
-- SQL válido en MySQL 8 y en H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway). Instantes en UTC (DATETIME(6)).
-- Una fila por turno y tipo (contrato con C-18): los reintentos suman en `intentos`, no crean filas nuevas.
-- `motivo_fallo` es una categoría cerrada; nunca se guarda el texto del proveedor (puede repetir la dirección).
-- Sin datos del paciente: la dirección se lee del paciente al enviar.

CREATE TABLE notificacion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_id       BIGINT      NOT NULL,
    tipo           VARCHAR(20) NOT NULL,
    canal          VARCHAR(10) NOT NULL,
    estado         VARCHAR(10) NOT NULL,
    intentos       INT         NOT NULL DEFAULT 0,
    motivo_fallo   VARCHAR(20) NULL,
    creada_en      DATETIME(6) NOT NULL,
    actualizada_en DATETIME(6) NOT NULL,
    enviada_en     DATETIME(6) NULL,
    CONSTRAINT uk_notificacion_turno_tipo UNIQUE (turno_id, tipo),
    CONSTRAINT ck_notificacion_tipo CHECK (tipo IN ('confirmacion', 'recordatorio')),
    CONSTRAINT ck_notificacion_canal CHECK (canal IN ('email')),
    CONSTRAINT ck_notificacion_estado CHECK (estado IN ('en_curso', 'enviada', 'fallida', 'omitida')),
    CONSTRAINT ck_notificacion_intentos CHECK (intentos >= 0),
    CONSTRAINT ck_notificacion_motivo CHECK (motivo_fallo IS NULL
        OR motivo_fallo IN ('conexion', 'autenticacion', 'destinatario', 'desconocido')),
    CONSTRAINT ck_notificacion_enviada CHECK (
        (estado = 'enviada' AND enviada_en IS NOT NULL) OR (estado <> 'enviada' AND enviada_en IS NULL)),
    CONSTRAINT ck_notificacion_fallida CHECK (
        (estado = 'fallida' AND motivo_fallo IS NOT NULL) OR (estado <> 'fallida' AND motivo_fallo IS NULL)),
    CONSTRAINT fk_notificacion_turno FOREIGN KEY (turno_id) REFERENCES turno (id)
) ENGINE=InnoDB;

-- La clave única (turno_id, tipo) ya sirve a la FK. Este índice es para C-18: fallidas o en_curso por antigüedad.
CREATE INDEX ix_notificacion_estado_actualizada ON notificacion (estado, actualizada_en);
