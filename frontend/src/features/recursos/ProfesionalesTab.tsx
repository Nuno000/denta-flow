import { useState } from 'react'
import { Dialogo } from '../../components/formulario/Dialogo'
import { CLASES_ACCION } from '../../components/formulario/estilos'
import { Celda } from '../../components/tabla/Celda'
import {
  useActivarProfesional,
  useDesactivarProfesional,
  usePrestaciones,
  useProfesionales,
  type Prestacion,
  type Profesional,
} from './api'
import { useAvisos, useEsAdministrador } from './avisosYErrores'
import { Avisos, ConfirmarDesactivacion, EncabezadoDeTab, Estado, MensajeDeError } from './comunes'
import { DialogoPrestaciones } from './DialogoPrestaciones'
import { FormularioProfesional } from './FormularioProfesional'
import { FilaDeRecurso, TablaDeRecursos } from './TablaDeRecursos'

type DialogoAbierto =
  | { tipo: 'alta' }
  | { tipo: 'editar' | 'desactivar' | 'prestaciones'; profesional: Profesional }

function nombreCompleto(profesional: Profesional): string {
  return `${profesional.nombre} ${profesional.apellido}`
}

/** Nombres de las prestaciones que realiza, en el orden del catálogo; las inactivas, rotuladas. */
function nombresDePrestaciones(profesional: Profesional, prestaciones: Prestacion[] | undefined): string {
  if (profesional.prestacionIds.length === 0) {
    return '—'
  }
  if (prestaciones === undefined) {
    return '…'
  }
  return prestaciones
    .filter((prestacion) => profesional.prestacionIds.includes(prestacion.id))
    .map((prestacion) => (prestacion.activa ? prestacion.nombre : `${prestacion.nombre} (inactiva)`))
    .join(', ')
}

/** Pestaña Profesionales: datos, estado y prestaciones que realiza cada uno (US-001). */
export function ProfesionalesTab() {
  const esAdmin = useEsAdministrador()
  const profesionales = useProfesionales()
  const prestaciones = usePrestaciones()
  const desactivar = useDesactivarProfesional()
  const activar = useActivarProfesional()
  const avisos = useAvisos()
  const [dialogo, setDialogo] = useState<DialogoAbierto | null>(null)

  const cerrar = () => setDialogo(null)
  const terminar = (mensaje: string) => {
    setDialogo(null)
    avisos.anunciar(mensaje)
  }
  const reactivar = async (profesional: Profesional) => {
    try {
      await activar.mutateAsync(profesional.id)
      avisos.anunciar('Profesional reactivado')
    } catch (causa) {
      avisos.fallar(causa)
    }
  }
  const confirmarDesactivacion = async (profesional: Profesional) => {
    try {
      await desactivar.mutateAsync(profesional.id)
      terminar('Profesional desactivado')
    } catch (causa) {
      setDialogo(null)
      avisos.fallar(causa)
    }
  }

  const columnas = ['Nombre', 'Email', 'Matrícula', 'Prestaciones', 'Estado']

  return (
    <section className="flex flex-col gap-3">
      <EncabezadoDeTab
        titulo="Profesionales"
        alta={esAdmin ? { texto: 'Nuevo profesional', alPulsar: () => setDialogo({ tipo: 'alta' }) } : undefined}
      />
      <Avisos aviso={avisos.aviso} error={avisos.error} />
      {profesionales.isPending && <p className="text-slate-700">Cargando profesionales…</p>}
      {profesionales.isError && (
        <MensajeDeError>No pudimos cargar los profesionales. Probá de nuevo en unos minutos.</MensajeDeError>
      )}
      {profesionales.data !== undefined && (
        <TablaDeRecursos etiqueta="Profesionales del consultorio" columnas={esAdmin ? [...columnas, 'Acciones'] : columnas}>
          {profesionales.data.map((profesional) => {
            const nombre = nombreCompleto(profesional)
            return (
              <FilaDeRecurso key={profesional.id}>
                <Celda etiqueta="Nombre">
                  <span className="font-medium text-slate-900 md:whitespace-nowrap">{nombre}</span>
                </Celda>
                <Celda etiqueta="Email">
                  <span className="break-all md:whitespace-nowrap md:break-normal">{profesional.email}</span>
                </Celda>
                <Celda etiqueta="Matrícula">{profesional.matricula ?? '—'}</Celda>
                <Celda etiqueta="Prestaciones">{nombresDePrestaciones(profesional, prestaciones.data)}</Celda>
                <Celda etiqueta="Estado">
                  <Estado activo={profesional.activo} />
                </Celda>
                {esAdmin && (
                  <Celda etiqueta="Acciones">
                    <span className="flex flex-wrap gap-2 md:flex-nowrap">
                      <button
                        type="button"
                        aria-label={`Editar ${nombre}`}
                        onClick={() => setDialogo({ tipo: 'editar', profesional })}
                        className={CLASES_ACCION}
                      >
                        Editar
                      </button>
                      <button
                        type="button"
                        aria-label={`Prestaciones de ${nombre}`}
                        disabled={prestaciones.data === undefined}
                        onClick={() => setDialogo({ tipo: 'prestaciones', profesional })}
                        className={CLASES_ACCION}
                      >
                        Prestaciones
                      </button>
                      {profesional.activo ? (
                        <button
                          type="button"
                          aria-label={`Desactivar ${nombre}`}
                          onClick={() => setDialogo({ tipo: 'desactivar', profesional })}
                          className={CLASES_ACCION}
                        >
                          Desactivar
                        </button>
                      ) : (
                        <button
                          type="button"
                          aria-label={`Reactivar ${nombre}`}
                          disabled={activar.isPending}
                          onClick={() => void reactivar(profesional)}
                          className={CLASES_ACCION}
                        >
                          Reactivar
                        </button>
                      )}
                    </span>
                  </Celda>
                )}
              </FilaDeRecurso>
            )
          })}
        </TablaDeRecursos>
      )}

      {dialogo?.tipo === 'alta' && (
        <Dialogo titulo="Nuevo profesional" alCerrar={cerrar}>
          <FormularioProfesional alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'editar' && (
        <Dialogo titulo={`Editar ${nombreCompleto(dialogo.profesional)}`} alCerrar={cerrar}>
          <FormularioProfesional profesional={dialogo.profesional} alTerminar={terminar} alCancelar={cerrar} />
        </Dialogo>
      )}
      {dialogo?.tipo === 'prestaciones' && prestaciones.data !== undefined && (
        <DialogoPrestaciones
          profesional={dialogo.profesional}
          prestaciones={prestaciones.data}
          alTerminar={terminar}
          alCerrar={cerrar}
        />
      )}
      {dialogo?.tipo === 'desactivar' && (
        <ConfirmarDesactivacion
          nombre={nombreCompleto(dialogo.profesional)}
          pendiente={desactivar.isPending}
          alConfirmar={() => void confirmarDesactivacion(dialogo.profesional)}
          alCerrar={cerrar}
        />
      )}
    </section>
  )
}
