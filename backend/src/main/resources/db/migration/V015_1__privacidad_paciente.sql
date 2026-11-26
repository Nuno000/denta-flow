-- C-15 privacidad-ley-25326 (design D2, revisado el 2026-11-26): anonimización/baja del paciente (PA-12).
-- SQL válido en MySQL 8 y en H2 MODE=MySQL. No edita V009_1: agrega columnas y restricciones.
--
-- Al anonimizar, el DNI queda vacío (NULL) y se registra quién y cuándo. Un paciente activo sigue con DNI
-- obligatorio y único (PA-13): solo un anonimizado puede quedar sin DNI. UNIQUE admite varios NULL en ambos
-- motores, así que los anonimizados no chocan entre sí y el DNI original queda libre para una reserva nueva.

ALTER TABLE paciente MODIFY dni VARCHAR(8) NULL;

ALTER TABLE paciente ADD COLUMN anonimizado_en DATETIME(6) NULL;

ALTER TABLE paciente ADD COLUMN anonimizado_por BIGINT NULL;

ALTER TABLE paciente ADD CONSTRAINT fk_paciente_anonimizado_por
    FOREIGN KEY (anonimizado_por) REFERENCES usuario (id);

ALTER TABLE paciente ADD CONSTRAINT ck_paciente_anonimizado CHECK (
    (anonimizado_en IS NULL AND anonimizado_por IS NULL)
    OR (anonimizado_en IS NOT NULL AND anonimizado_por IS NOT NULL));

ALTER TABLE paciente ADD CONSTRAINT ck_paciente_dni_o_anonimizado CHECK (
    dni IS NOT NULL OR anonimizado_en IS NOT NULL);
