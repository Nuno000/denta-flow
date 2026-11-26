import { expect, test } from '@playwright/test'

/**
 * Spec despliegue-publico, "Flujos del corte verificados de punta a punta": ingreso del administrador,
 * menú según el rol y los recursos de demostración (C-04, C-05). Requiere el backend con seed de demo.
 */
const email = process.env.E2E_ADMIN_EMAIL
const contrasena = process.env.E2E_ADMIN_PASSWORD

test.describe('Panel del administrador', () => {
  test.skip(email === undefined || contrasena === undefined, 'Definí E2E_ADMIN_EMAIL y E2E_ADMIN_PASSWORD')

  test.beforeEach(async ({ page }) => {
    await page.goto('/ingresar')
    await page.getByLabel('Email').fill(email ?? '')
    await page.getByLabel('Contraseña').fill(contrasena ?? '')
    await page.getByRole('button', { name: 'Ingresar' }).click()
    await expect(page).toHaveURL(/\/panel$/)
  })

  test('ingresa y ve el menú completo del administrador', async ({ page }) => {
    const menu = page.getByRole('navigation')
    await expect(menu.getByRole('link', { name: 'Recursos' })).toBeVisible()
    await expect(menu.getByRole('link', { name: 'Usuarios' })).toBeVisible()
  })

  test('ve los profesionales de demostración', async ({ page }) => {
    await page.getByRole('navigation').getByRole('link', { name: 'Recursos' }).click()
    await expect(page.getByRole('heading', { level: 1, name: 'Recursos' })).toBeVisible()
    await expect(page.getByText('Laura', { exact: false }).first()).toBeVisible()
  })

  test('una ruta profunda carga la app al recargar', async ({ page }) => {
    await page.goto('/panel/recursos')
    await page.reload()
    await expect(page.getByRole('heading', { level: 1, name: 'Recursos' })).toBeVisible()
  })
})

test('credenciales inválidas muestran un error sin entrar al panel', async ({ page }) => {
  await page.goto('/ingresar')
  await page.getByLabel('Email').fill('nadie@example.com')
  await page.getByLabel('Contraseña').fill('contrasena-incorrecta')
  await page.getByRole('button', { name: 'Ingresar' }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page).toHaveURL(/\/ingresar$/)
})
