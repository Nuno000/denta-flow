import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { StrictMode, useState } from 'react'
import { describe, expect, it } from 'vitest'
import { Dialogo } from './Dialogo'

function Pantalla() {
  const [abierto, setAbierto] = useState(false)
  return (
    <>
      <button type="button" onClick={() => setAbierto(true)}>
        Abrir
      </button>
      {abierto && (
        <Dialogo titulo="Prueba" alCerrar={() => setAbierto(false)}>
          <button type="button" onClick={() => setAbierto(false)}>
            Cerrar
          </button>
        </Dialogo>
      )}
    </>
  )
}

describe('Dialogo', () => {
  it('lleva el foco adentro al abrir y lo devuelve a quien lo abrió al cerrar, también en StrictMode', async () => {
    const usuario = userEvent.setup()
    render(
      <StrictMode>
        <Pantalla />
      </StrictMode>,
    )
    const abrir = screen.getByRole('button', { name: 'Abrir' })

    await usuario.click(abrir)
    expect(screen.getByRole('button', { name: 'Cerrar' })).toHaveFocus()

    await usuario.click(screen.getByRole('button', { name: 'Cerrar' }))
    expect(abrir).toHaveFocus()
  })

  it('Escape cierra y devuelve el foco', async () => {
    const usuario = userEvent.setup()
    render(<Pantalla />)
    const abrir = screen.getByRole('button', { name: 'Abrir' })

    await usuario.click(abrir)
    await usuario.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
    expect(abrir).toHaveFocus()
  })
})
