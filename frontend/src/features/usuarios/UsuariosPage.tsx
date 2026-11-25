import { useState } from 'react'
import { BarraDelPanel } from '../auth/BarraDelPanel'
import { useSesion } from '../auth/contextoSesion'
import {
  leerErrorDeApi,
  useActivar,
  useDesactivar,
  useProfesionalesDisponibles,
  useRoles,
  useUsuarios,
  type UsuarioGestionado,
} from './api'
import { CLASES_BOTON_PRIMARIO, CLASES_BOTON_SECUNDARIO } from './estilosFormulario'
import { Dialogo } from './Dialogo'
import { FormularioBlanqueo } from './FormularioBlanqueo'
import { FormularioCambioRol } from './FormularioCambioRol'
import { FormularioUsuario } from './FormularioUsuario'

type DialogoAbierto =
  | { tipo: 'alta' }
  | { tipo: 'rol'; usuario: UsuarioGestionado }
  | { tipo: 'desactivar'; usuario: UsuarioGestionado }
  | { tipo: 'blanqueo'; usuario: UsuarioGestionado }

const CLASES_ACCION =
  'rounded-md border border-slate-400 px-2 py-1 text-sm font-medium text-slate-900 hover:bg-slate-100 focus:outline-none focus-visible:ring-2 focus-visible:ring-sky-700'

/** Celda que en el celular muestra su encabezado antes del valor (la tabla se apila como tarjetas). */
function Celda({ etiqueta, children }: { etiqueta: string; children: React.ReactNode }) {
  return (
    <td
      role="cell"
      data-etiqueta={etiqueta}
      className="flex gap-2 px-1 py-0.5 before:min-w-28 before:font-medium before:text-slate-700 before:content-[attr(data-etiqueta)] md:table-cell md:px-3 md:py-2 md:before:content-none"
    >
      {children}
    </td>
  )
}

/**
 * ABM simple de usuarios para el administrador (US-003, design D13). Una sola tabla semántica que en el
 * celular se apila como tarjetas por CSS, así lectores de pantalla y tests ven una sola estructura.
 */
export function UsuariosPage() {
  const { usuario: actual } = useSesion()
  const usuarios = useUsuarios()
  const roles = useRoles()
  const profesionales = useProfesionalesDisponibles()
  const activar = useActivar()
  const desactivar = useDesactivar()
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)
  const [aviso, setAviso] = useState('')
  const [errorAccion, setErrorAccion] = useState<string | null>(null)

  const nombreDeRol = (codigo: string) => roles.data?.find((rol) => rol.codigo === codigo)?.nombre ?? codigo
  const cerrar = () => setDialogo(null)
  const terminar = (mensaje: string) => {
    setDialogo(null)
    setAviso(mensaje)
  }

  const reactivar = async (usuario: UsuarioGestionado) => {
    setErrorAccion(null)
    try {
      await activar.mutateAsync(usuario.id)
      setAviso('Usuario reactivado')
    } catch (error) {
      setErrorAccion(leerErrorDeApi(error).general)
    }
  }

  const confirmarDesactivacion = async (usuario: UsuarioGestionado) => {
    setErrorAccion(null)
    try {
      await desactivar.mutateAsync(usuario.id)
      terminar('Usuario desactivado')
    } catch (error) {
      setDialogo(null)
      setErrorAccion(leerErrorDeApi(error).general)
    }
  }

  return (
    <>
      <BarraDelPanel />
      <main className="mx-auto flex max-w-5xl flex-col gap-4 p-4 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold text-slate-900">Usuarios</h1>
          <button type="button" onClick={() => setDialogo({ tipo: 'alta' })} className={CLASES_BOTON_PRIMARIO}>
            Nuevo usuario
          </button>
        </div>

        <p role="status" aria-live="polite" className="min-h-6 text-sm font-medium text-green-800">
          {aviso}
        </p>
        {errorAccion !== null && (
          <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
            {errorAccion}
          </p>
        )}

        {usuarios.isPending && <p className="text-slate-700">Cargando usuarios…</p>}
        {usuarios.isError && (
          <p role="alert" className="rounded-md border border-red-700 bg-red-50 p-3 text-sm text-red-800">
            No pudimos cargar los usuarios. Probá de nuevo en unos minutos.
          </p>
        )}

        {usuarios.data !== undefined && (
          <table role="table" aria-label="Usuarios del consultorio" className="w-full text-left text-sm md:table">
            <thead className="hidden border-b border-slate-300 md:table-header-group">
              <tr role="row">
                <th role="columnheader" scope="col" className="px-3 py-2">Email</th>
                <th role="columnheader" scope="col" className="px-3 py-2">Rol</th>
                <th role="columnheader" scope="col" className="px-3 py-2">Profesional</th>
                <th role="columnheader" scope="col" className="px-3 py-2">Estado</th>
                <th role="columnheader" scope="col" className="px-3 py-2">Acciones</th>
              </tr>
            </thead>
            <tbody className="flex flex-col gap-3 md:table-row-group">
              {usuarios.data.map((usuario) => {
                const esPropio = usuario.id === actual?.id
                return (
                  <tr
                    role="row"
                    key={usuario.id}
                    className="flex flex-col rounded-lg border border-slate-300 p-3 md:table-row md:rounded-none md:border-0 md:border-b md:p-0"
                  >
                    <Celda etiqueta="Email">
                      <span className="break-all text-slate-900">{usuario.email}</span>
                      {esPropio && <span className="text-slate-700">(vos)</span>}
                    </Celda>
                    <Celda etiqueta="Rol">{nombreDeRol(usuario.rol)}</Celda>
                    <Celda etiqueta="Profesional">{usuario.profesionalNombre ?? '—'}</Celda>
                    <Celda etiqueta="Estado">
                      <span className={usuario.activo ? 'text-green-800' : 'text-slate-700'}>
                        {usuario.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </Celda>
                    <Celda etiqueta="Acciones">
                      {!esPropio && (
                        <span className="flex flex-wrap gap-2">
                          <button
                            type="button"
                            aria-label={`Cambiar rol de ${usuario.email}`}
                            onClick={() => setDialogo({ tipo: 'rol', usuario })}
                            className={CLASES_ACCION}
                          >
                            Cambiar rol
                          </button>
                          {usuario.activo ? (
                            <button
                              type="button"
                              aria-label={`Desactivar a ${usuario.email}`}
                              onClick={() => setDialogo({ tipo: 'desactivar', usuario })}
                              className={CLASES_ACCION}
                            >
                              Desactivar
                            </button>
                          ) : (
                            <button
                              type="button"
                              aria-label={`Reactivar a ${usuario.email}`}
                              onClick={() => void reactivar(usuario)}
                              className={CLASES_ACCION}
                            >
                              Reactivar
                            </button>
                          )}
                          <button
                            type="button"
                            aria-label={`Blanquear contraseña de ${usuario.email}`}
                            onClick={() => setDialogo({ tipo: 'blanqueo', usuario })}
                            className={CLASES_ACCION}
                          >
                            Blanquear contraseña
                          </button>
                        </span>
                      )}
                    </Celda>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </main>

      {dialogo?.tipo === 'alta' && (
        <Dialogo titulo="Nuevo usuario" alCerrar={cerrar}>
          <FormularioUsuario
            roles={roles.data ?? []}
            profesionales={profesionales.data ?? []}
            alTerminar={terminar}
            alCancelar={cerrar}
          />
        </Dialogo>
      )}
      {dialogo?.tipo === 'rol' && (
        <Dialogo titulo={`Cambiar rol de ${dialogo.usuario.email}`} alCerrar={cerrar}>
          <FormularioCambioRol
            usuario={dialogo.usuario}
            roles={roles.data ?? []}
            profesionales={profesionales.data ?? []}
            alTerminar={terminar}
            alCancelar={cerrar}
          />
        </Dialogo>
      )}
      {dialogo?.tipo === 'blanqueo' && (
        <Dialogo titulo={`Blanquear contraseña de ${dialogo.usuario.email}`} alCerrar={cerrar}>
          <FormularioBlanqueo usuario={dialogo.usuario} alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'desactivar' && (
        <Dialogo titulo={`¿Desactivar a ${dialogo.usuario.email}?`} alCerrar={cerrar}>
          <p className="text-slate-700">No va a poder ingresar hasta que lo reactives. Sus datos se conservan.</p>
          <div className="flex flex-wrap justify-end gap-2">
            <button type="button" onClick={cerrar} className={CLASES_BOTON_SECUNDARIO}>
              Cancelar
            </button>
            <button
              type="button"
              disabled={desactivar.isPending}
              onClick={() => void confirmarDesactivacion(dialogo.usuario)}
              className={CLASES_BOTON_PRIMARIO}
            >
              Desactivar
            </button>
          </div>
        </Dialogo>
      )}
    </>
  )
}
