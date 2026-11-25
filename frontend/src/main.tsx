import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter } from 'react-router-dom'
import './index.css'
import { Proveedores } from './Proveedores'
import { rutas } from './routes/rutas'

const router = createBrowserRouter(rutas)

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Proveedores router={router} />
  </StrictMode>,
)
