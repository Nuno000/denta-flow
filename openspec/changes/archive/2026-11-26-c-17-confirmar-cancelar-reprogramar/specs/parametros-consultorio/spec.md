## MODIFIED Requirements

### Requirement: Anticipación mínima en horas enteras dentro de rango
La anticipación mínima de cancelación/reprogramación (RN-CA-01, RN-CA-02, PA-04, DD-12) SHALL ser un número entero de horas entre 0 y 168 (una semana), ambos incluidos. Un valor de 0 horas SHALL permitir cancelar o reprogramar hasta el inicio exacto del turno. El sistema MUST rechazar con 400 cualquier otro valor y MUST dejar el valor anterior sin cambios.

#### Scenario: Valor válido
- **WHEN** un administrador envía `PUT /api/parametros/anticipacion-minima-horas` con valor `48`
- **THEN** recibe 200 con el valor `48` y las lecturas siguientes devuelven 48 horas

#### Scenario: Límites del rango
- **WHEN** un administrador guarda el valor `0` y después el valor `168`
- **THEN** ambos se aceptan

#### Scenario: Fuera de rango
- **WHEN** un administrador envía el valor `-1`, `-5` o `169`
- **THEN** recibe 400 con un detalle en español que indica el rango permitido (de 0 a 168 horas) y el valor guardado sigue siendo el anterior

#### Scenario: Valor no entero
- **WHEN** un administrador envía el valor `veinte`, `24.5`, un texto vacío o no envía el valor
- **THEN** recibe 400 y el valor guardado sigue siendo el anterior
