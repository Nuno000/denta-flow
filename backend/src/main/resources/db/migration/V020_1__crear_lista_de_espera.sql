-- C-20 lista-de-espera: tabla lista_espera y parámetro lista-espera-plazo-oferta-horas.
-- SQL válido en MySQL 8 (InnoDB) y en H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway).
-- Fechas y horas de timestamps en UTC (DATETIME(6)); fechas y horas deseadas en horario local del consultorio.

CREATE TABLE lista_espera (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    paciente_id             BIGINT      NOT NULL,
    prestacion_id           BIGINT      NOT NULL,
    profesional_id          BIGINT      NULL,
    fecha_desde             DATE        NOT NULL,
    fecha_hasta             DATE        NOT NULL,
    hora_desde              TIME        NULL,
    hora_hasta              TIME        NULL,
    estado                  VARCHAR(20) NOT NULL,
    turno_origen_id         BIGINT      NULL,
    profesional_ofrecido_id BIGINT      NULL,
    inicio_ofrecido         DATETIME(6) NULL,
    token_aceptacion        VARCHAR(64) NULL,
    ofrecida_en             DATETIME(6) NULL,
    ofrecida_hasta          DATETIME(6) NULL,
    turno_creado_id         BIGINT      NULL,
    creado_por_usuario_id   BIGINT      NOT NULL,
    creado_en               DATETIME(6) NOT NULL,
    CONSTRAINT uk_lista_espera_token UNIQUE (token_aceptacion),
    CONSTRAINT ck_lista_espera_estado CHECK (estado IN ('activa', 'ofrecida', 'atendida', 'vencida')),
    CONSTRAINT ck_lista_espera_fechas CHECK (fecha_hasta >= fecha_desde),
    CONSTRAINT ck_lista_espera_horas CHECK (
        (hora_desde IS NULL AND hora_hasta IS NULL)
        OR (hora_desde IS NOT NULL AND hora_hasta IS NOT NULL AND hora_hasta > hora_desde)
    ),
    CONSTRAINT fk_lista_espera_paciente    FOREIGN KEY (paciente_id)             REFERENCES paciente (id),
    CONSTRAINT fk_lista_espera_prestacion  FOREIGN KEY (prestacion_id)           REFERENCES prestacion (id),
    CONSTRAINT fk_lista_espera_profesional FOREIGN KEY (profesional_id)          REFERENCES profesional (id),
    CONSTRAINT fk_lista_espera_prof_of     FOREIGN KEY (profesional_ofrecido_id) REFERENCES profesional (id),
    CONSTRAINT fk_lista_espera_turno_orig  FOREIGN KEY (turno_origen_id)         REFERENCES turno (id),
    CONSTRAINT fk_lista_espera_turno_crea  FOREIGN KEY (turno_creado_id)         REFERENCES turno (id),
    CONSTRAINT fk_lista_espera_creado_por  FOREIGN KEY (creado_por_usuario_id)   REFERENCES usuario (id)
) ENGINE=InnoDB;

CREATE INDEX ix_lista_espera_matching ON lista_espera (estado, prestacion_id, profesional_id, fecha_desde, fecha_hasta);
CREATE INDEX ix_lista_espera_vencimiento ON lista_espera (estado, ofrecida_hasta);

INSERT INTO parametro (clave, valor, provisorio) VALUES
    ('lista-espera-plazo-oferta-horas', '4', TRUE);
