package ar.dentaflow.aplicacion.pacientes;

import ar.dentaflow.dominio.pacientes.Dni;
import ar.dentaflow.dominio.pacientes.Paciente;
import ar.dentaflow.dominio.pacientes.PacienteRepositorio;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Alta y deduplicación de pacientes por DNI (design D8, Q2, Q11). Cada registro corre en <b>su propia
 * transacción</b>, antes y separada de la del turno ({@code ServicioDeTurnos.crear}): un conflicto del turno no
 * deshace al paciente, y una carrera de dos altas del mismo DNI nuevo no aborta la reserva.
 *
 * <p>Política según el origen: la reserva online <b>no pisa</b> los datos de contacto de un paciente existente
 * (cualquiera con un DNI ajeno podría cambiar su email); recepción, personal autenticado, los <b>reemplaza</b>. Si
 * llega {@code consentimientoEn} reemplaza la fecha de consentimiento en ambos casos.
 *
 * <p>Si dos altas del mismo DNI nuevo compiten, la segunda choca con {@code uk_paciente_dni}: se captura fuera de
 * la transacción fallida, se relee en una nueva y se aplica la política. Un reintento; si vuelve a fallar se
 * propaga.
 */
@Service
public class ServicioDePacientes {

    private final PacienteRepositorio pacientes;
    private final Clock reloj;
    private final TransactionTemplate transaccionNueva;

    public ServicioDePacientes(PacienteRepositorio pacientes, Clock reloj,
            PlatformTransactionManager administradorDeTransacciones) {
        this.pacientes = pacientes;
        this.reloj = reloj;
        this.transaccionNueva = new TransactionTemplate(administradorDeTransacciones);
        this.transaccionNueva.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Reserva online: crea al paciente si el DNI es nuevo; si existe lo devuelve sin cambiar su contacto. */
    public Paciente registrarDesdeReservaOnline(DatosPaciente datos) {
        return registrar(datos, false);
    }

    /** Recepción: crea al paciente si el DNI es nuevo; si existe reemplaza su contacto con los datos recibidos. */
    public Paciente registrarDesdeRecepcion(DatosPaciente datos) {
        return registrar(datos, true);
    }

    /** Busca por DNI (se normaliza antes de buscar). */
    public Optional<Paciente> buscarPorDni(String dni) {
        return pacientes.buscarPorDni(Dni.normalizar(dni));
    }

    private Paciente registrar(DatosPaciente datos, boolean reemplazaContacto) {
        String dni = Dni.normalizar(datos.dni());
        Supplier<Paciente> registro = () -> transaccionNueva.execute(estado -> aplicar(dni, datos, reemplazaContacto));
        try {
            return registro.get();
        } catch (DataIntegrityViolationException carrera) {
            return registro.get();
        }
    }

    private Paciente aplicar(String dni, DatosPaciente datos, boolean reemplazaContacto) {
        Optional<Paciente> existente = pacientes.buscarPorDni(dni);
        if (existente.isEmpty()) {
            Paciente nuevo = Paciente.nuevo(dni, datos.nombre(), datos.apellido(), datos.telefono(), datos.email(),
                    reloj.instant());
            if (datos.consentimientoEn() != null) {
                nuevo.registrarConsentimiento(datos.consentimientoEn());
            }
            return pacientes.guardar(nuevo);
        }
        Paciente paciente = existente.get();
        boolean cambio = false;
        if (reemplazaContacto) {
            paciente.actualizarContacto(datos.nombre(), datos.apellido(), datos.telefono(), datos.email());
            cambio = true;
        }
        if (datos.consentimientoEn() != null) {
            paciente.registrarConsentimiento(datos.consentimientoEn());
            cambio = true;
        }
        return cambio ? pacientes.guardar(paciente) : paciente;
    }
}
