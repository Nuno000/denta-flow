# Accesibilidad y Mobile-First — C-16 `reserva-online-publica`

Fecha: 2026-11-26  
Dispositivo objetivo: 390×844 px (móvil) y navegación por teclado (WCAG 2.1 AA)

## 1. Revisión de componentes en 390×844 px

- **Disposición en una sola columna**:
  - Toda la pantalla `/reservar` y sus pasos (`PasoPrestacion`, `PasoProfesional`, `PasoHorario`, `PasoDatos`, `PasoConfirmacion`) se adaptan a 390 px de ancho sin desbordes horizontales.
  - Los botones de selección de días, horarios y acciones tienen un tamaño objetivo mínimo de 44×44 px garantizando facilidad táctil en pantallas de celular.
- **Componente AvisoPrivacidad (trasladado de C-15 4.4)**:
  - Casilla de verificación con etiqueta visible y botón para expandir/colapsar el texto completo del aviso.
  - Totalmente legible y táctil a 390 px de ancho.

## 2. Navegación por teclado y foco

- **Orden de foco lógico**:
  - Al avanzar de paso, el foco se desplaza automáticamente al encabezado `h2` del paso actual (`TituloDePaso.tsx`), anunciando a los lectores de pantalla el nuevo estado del flujo.
  - Al presionar "Volver", el foco regresa al paso anterior conservando la selección realizada.
  - Ante errores de validación en el formulario, el foco se posiciona automáticamente en el primer campo inválido.
- **Atributos accesibles (ARIA)**:
  - Los errores de campo están asociados a través de `aria-describedby` y los campos marcan `aria-invalid="true"`.
  - Las alertas de error 409 y 429 utilizan `role="alert"` para notificación inmediata por lectores de pantalla.
