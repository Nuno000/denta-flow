import { defineConfig, devices } from '@playwright/test'

/**
 * E2E de los flujos del corte (C-19, design D6). Corren contra la SPA ya levantada:
 * - local: `scripts/demo-local.ps1` o `npm run dev` + backend en `dev` (URL por defecto);
 * - desplegado: `E2E_BASE_URL=https://<frontend>.vercel.app npm run e2e`.
 * Localmente usa el Chrome instalado (sin descargar navegadores); en CI, el Chromium de Playwright.
 * Credenciales del administrador por E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD (nunca en el repo).
 */
const enCi = process.env.CI !== undefined

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  forbidOnly: enCi,
  retries: enCi ? 1 : 0,
  reporter: enCi ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:5173',
    channel: enCi ? undefined : 'chrome',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'escritorio', use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } } },
    { name: 'movil', use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 }, isMobile: false, hasTouch: true } },
  ],
})
