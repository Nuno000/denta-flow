import { useContacto } from './reservaApi'

/** Contacto del consultorio (parámetro `contacto-consultorio`), para cuando no se puede reservar online. */
export function ContactoDelConsultorio() {
  const contacto = useContacto()

  if (contacto.isSuccess) {
    return <p className="font-medium text-slate-900">{contacto.data.contacto}</p>
  }
  if (contacto.isError) {
    return <p>Comunicate con el consultorio para coordinar tu turno.</p>
  }
  return null
}
