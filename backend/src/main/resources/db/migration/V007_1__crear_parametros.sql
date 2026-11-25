-- C-07 parametros-consultorio: parámetros de configuración del consultorio (clave-valor con catálogo
-- cerrado en el dominio). SQL válido en MySQL 8 y en H2 MODE=MySQL. Archivo en UTF-8 (default de
-- Flyway) por las tildes y la "ñ" de los textos iniciales.
-- Los valores iniciales son datos de referencia: existen en todo entorno (como los roles de V004_1) y
-- nacen provisorios. Reemplazar uno por un valor definitivo se hace con una V007_n nueva (README).
-- El tipo y el rango de cada clave se validan en el dominio (ClaveParametro), no con un CHECK por clave.

CREATE TABLE parametro (
    clave           VARCHAR(60)   NOT NULL,
    valor           VARCHAR(4000) NOT NULL,
    provisorio      BOOLEAN       NOT NULL,
    actualizado_en  DATETIME(6)   NULL,
    actualizado_por BIGINT        NULL,
    CONSTRAINT pk_parametro PRIMARY KEY (clave),
    CONSTRAINT fk_parametro_actualizado_por FOREIGN KEY (actualizado_por) REFERENCES usuario (id),
    CONSTRAINT ck_parametro_autor_y_fecha CHECK (
        (actualizado_en IS NULL AND actualizado_por IS NULL)
        OR (actualizado_en IS NOT NULL AND actualizado_por IS NOT NULL))
) ENGINE=InnoDB;

INSERT INTO parametro (clave, valor, provisorio) VALUES
    ('anticipacion-minima-horas', '24', TRUE),
    ('aviso-privacidad-texto',
     'Este texto es un borrador pendiente de revisión legal. Los datos que nos informás (DNI, nombre, teléfono y email) se usan solamente para gestionar tus turnos: confirmarlos, recordártelos y avisarte si cambian. No se ceden a terceros. Podés pedir en cualquier momento el acceso, la rectificación o la supresión de tus datos, según los artículos 14 a 16 de la Ley 25.326. La Agencia de Acceso a la Información Pública es el órgano de control de esa ley y atiende las denuncias y reclamos sobre el incumplimiento de las normas de protección de datos personales.',
     TRUE),
    ('aviso-privacidad-leyenda', 'Leí y acepto el aviso de privacidad', TRUE);
