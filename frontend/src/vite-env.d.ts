/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base de la API (pública: queda en el bundle). Ej.: http://localhost:8080 */
  readonly VITE_API_BASE_URL?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
