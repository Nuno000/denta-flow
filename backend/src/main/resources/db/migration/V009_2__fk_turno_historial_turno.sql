-- C-09 modelo-turno-paciente: cierra el pendiente de C-08 (design D2). El historial pasa a estar ligado a un
-- turno existente. SQL válido en MySQL 8 y en H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway).
-- NO borra historial huérfano: la auditoría nunca se borra desde una migración. Si una base local de desarrollo
-- tiene filas de prueba sueltas en turno_historial, esta migración falla de forma visible y se limpian a mano
-- (ver db/migration/README.md). El índice ix_turno_historial_turno (turno_id, ...) ya sirve a la FK.

ALTER TABLE turno_historial
    ADD CONSTRAINT fk_turno_historial_turno FOREIGN KEY (turno_id) REFERENCES turno (id);
