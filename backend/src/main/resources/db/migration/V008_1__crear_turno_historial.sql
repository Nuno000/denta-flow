-- C-08 auditoria-cambios: historial solo-inserción de los cambios de cada turno (RN-AU-01, RN-SO-02).
-- SQL válido en MySQL 8 y en H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway).
-- turno_id NO tiene FK porque la tabla turno nace en C-09: C-09 agrega fk_turno_historial_turno en su
-- propia migración (design D1). La aplicación nunca actualiza ni borra filas (design D4); los permisos
-- del usuario de la base en producción quedan para C-19.
-- ocurrido_en en UTC. valor_anterior y valor_nuevo son texto JSON con los campos que cambiaron.

CREATE TABLE turno_historial (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_id       BIGINT        NOT NULL,
    actor_tipo     VARCHAR(10)   NOT NULL,
    usuario_id     BIGINT        NULL,
    ocurrido_en    DATETIME(6)   NOT NULL,
    accion         VARCHAR(20)   NOT NULL,
    valor_anterior VARCHAR(4000) NULL,
    valor_nuevo    VARCHAR(4000) NULL,
    CONSTRAINT ck_turno_historial_actor_tipo CHECK (actor_tipo IN ('usuario', 'paciente', 'sistema')),
    CONSTRAINT ck_turno_historial_actor_usuario CHECK (
        (actor_tipo = 'usuario' AND usuario_id IS NOT NULL)
        OR (actor_tipo <> 'usuario' AND usuario_id IS NULL)),
    CONSTRAINT ck_turno_historial_accion CHECK (accion IN
        ('creacion', 'confirmacion', 'cancelacion', 'reprogramacion', 'cambio_estado')),
    CONSTRAINT ck_turno_historial_valores CHECK (valor_anterior IS NOT NULL OR valor_nuevo IS NOT NULL),
    CONSTRAINT fk_turno_historial_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id)
) ENGINE=InnoDB;

-- Sirve a la única consulta (historial de un turno en orden) y da el desempate estable por id.
CREATE INDEX ix_turno_historial_turno ON turno_historial (turno_id, ocurrido_en, id);
