-- C-16 reserva-online-publica: contacto del consultorio para la reserva pública (design D1, spec
-- parametros-consultorio "Contacto del consultorio obligatorio y acotado"). Dato de referencia, provisorio hasta que
-- el administrador lo reemplace. SQL válido en MySQL 8 y en H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway).

INSERT INTO parametro (clave, valor, provisorio) VALUES
    ('contacto-consultorio', 'Comunicate con el consultorio por teléfono para coordinar tu turno.', TRUE);
