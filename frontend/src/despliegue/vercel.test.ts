import { describe, expect, it } from 'vitest'
import contenido from '../../vercel.json?raw'

interface Reescritura {
  source: string
  destination: string
}

describe('vercel.json (spec despliegue-publico, "Rutas de la SPA en el hosting")', () => {
  it('reescribe toda ruta de la app a index.html', () => {
    const configuracion = JSON.parse(contenido) as { rewrites?: Reescritura[] }

    expect(configuracion.rewrites).toContainEqual({ source: '/(.*)', destination: '/index.html' })
  })
})
