import { render, screen } from '@testing-library/react'
import { createMemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { Proveedores } from '../Proveedores'
import { rutas } from './rutas'

describe('ruta /', () => {
  it('muestra el nombre DentaFlow dentro del QueryClientProvider y el router', async () => {
    const router = createMemoryRouter(rutas, { initialEntries: ['/'] })

    render(<Proveedores router={router} />)

    expect(await screen.findByRole('heading', { level: 1, name: 'DentaFlow' })).toBeInTheDocument()
  })
})
