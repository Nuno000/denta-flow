-- C-06 horarios-y-bloqueos: horario de atención semanal por profesional y bloqueos de agenda por
-- profesional, sillón o consultorio. SQL válido en MySQL 8 (>= 8.0.16 para los CHECK) y en H2
-- MODE=MySQL. Nombres de restricción únicos en todo el esquema (H2 no permite repetirlos).
--
-- horario_atencion: regla semanal en hora de reloj del consultorio (America/Argentina/Buenos_Aires),
-- por eso TIME sin zona y no DATETIME en UTC. dia_semana ISO-8601: 1 = lunes ... 7 = domingo.
-- La no superposición de rangos por profesional y día no se puede expresar en MySQL: la garantiza el
-- servicio bloqueando la fila del profesional (SELECT ... FOR UPDATE) antes de verificar y guardar.
--
-- bloqueo: instantes absolutos en UTC (DATETIME(6)), franja semiabierta [inicio, fin). Las FK no
-- llevan acciones ON DELETE porque MySQL las prohíbe en columnas usadas por un CHECK, y los
-- recursos no se borran (se desactivan).

CREATE TABLE horario_atencion (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    profesional_id BIGINT NOT NULL,
    dia_semana     INT    NOT NULL,
    hora_inicio    TIME   NOT NULL,
    hora_fin       TIME   NOT NULL,
    CONSTRAINT fk_horario_atencion_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT ck_horario_atencion_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT ck_horario_atencion_rango CHECK (hora_inicio < hora_fin)
) ENGINE=InnoDB;

CREATE INDEX idx_horario_atencion_profesional_dia ON horario_atencion (profesional_id, dia_semana, hora_inicio);

CREATE TABLE bloqueo (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ambito         VARCHAR(20)  NOT NULL,
    profesional_id BIGINT       NULL,
    sillon_id      BIGINT       NULL,
    inicio         DATETIME(6)  NOT NULL,
    fin            DATETIME(6)  NOT NULL,
    motivo         VARCHAR(20)  NOT NULL,
    detalle        VARCHAR(200) NULL,
    modificado_por BIGINT       NOT NULL,
    modificado_en  DATETIME(6)  NOT NULL,
    CONSTRAINT fk_bloqueo_profesional FOREIGN KEY (profesional_id) REFERENCES profesional (id),
    CONSTRAINT fk_bloqueo_sillon FOREIGN KEY (sillon_id) REFERENCES sillon (id),
    CONSTRAINT fk_bloqueo_modificado_por FOREIGN KEY (modificado_por) REFERENCES usuario (id),
    CONSTRAINT ck_bloqueo_ambito CHECK (ambito IN ('PROFESIONAL', 'SILLON', 'CONSULTORIO')),
    CONSTRAINT ck_bloqueo_motivo CHECK (motivo IN ('VACACIONES', 'FERIADO', 'AUSENCIA', 'OTRO')),
    CONSTRAINT ck_bloqueo_rango CHECK (inicio < fin),
    CONSTRAINT ck_bloqueo_recurso_segun_ambito CHECK (
        (ambito = 'PROFESIONAL' AND profesional_id IS NOT NULL AND sillon_id IS NULL)
        OR (ambito = 'SILLON' AND sillon_id IS NOT NULL AND profesional_id IS NULL)
        OR (ambito = 'CONSULTORIO' AND profesional_id IS NULL AND sillon_id IS NULL))
) ENGINE=InnoDB;

CREATE INDEX idx_bloqueo_profesional_rango ON bloqueo (profesional_id, inicio, fin);
CREATE INDEX idx_bloqueo_sillon_rango ON bloqueo (sillon_id, inicio, fin);
CREATE INDEX idx_bloqueo_ambito_rango ON bloqueo (ambito, inicio, fin);
