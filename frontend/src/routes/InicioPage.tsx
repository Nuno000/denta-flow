/** Página inicial mínima: solo prueba que la SPA arranca. Las pantallas reales llegan con sus changes. */
export function InicioPage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center gap-2 p-6">
      <h1 className="text-3xl font-bold">DentaFlow</h1>
      <p className="text-base">Turnos online para tu consultorio odontológico.</p>
    </main>
  )
}
