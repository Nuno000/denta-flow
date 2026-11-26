-- C-21 whatsapp-prellenado: plantilla del mensaje de WhatsApp que recepción abre desde la agenda (design D3 y D5,
-- spec parametros-consultorio "Plantilla de WhatsApp obligatoria y acotada"). Valor provisorio hasta que el
-- administrador lo reemplace. SQL válido en MySQL 8 y en H2 MODE=MySQL. Archivo en UTF-8 (default de Flyway).

INSERT INTO parametro (clave, valor, provisorio) VALUES
    ('whatsapp-plantilla', 'Hola {nombre}, te recordamos tu turno de {prestacion} con {profesional} el {fecha} a las {hora} (hora de Argentina). Para confirmarlo, cancelarlo o reprogramarlo entrá a {enlace}', TRUE);
