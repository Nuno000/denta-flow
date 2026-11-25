# DentaFlow — frontend

SPA React + Vite + TypeScript (`strict`) para la reserva pública y el panel interno. La puesta en marcha completa está en el [README de la raíz](../README.md).

```bash
cp .env.example .env   # VITE_API_BASE_URL
npm install
npm run dev            # http://localhost:5173
npm run lint           # oxlint (prohibido `any`)
npm test               # Vitest + Testing Library
npm run build
```

- Llamadas HTTP solo con `src/lib/apiClient.ts` y TanStack Query (nada de `fetch` en componentes).
- Formularios con React Hook Form + Zod. Estilos con Tailwind CSS.
- Código por funcionalidad en `src/features/{reserva,agenda,auth}`; rutas en `src/routes/`.
