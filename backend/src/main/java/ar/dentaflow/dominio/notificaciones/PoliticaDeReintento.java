package ar.dentaflow.dominio.notificaciones;

/**
 * Cuándo reintentar un envío (spec "Reintento ante fallas transitorias", design D3): hasta completar
 * {@value #INTENTOS_MAXIMOS} intentos en total, solo ante fallas transitorias. Las credenciales SMTP inválidas o
 * una dirección rechazada no se arreglan reintentando.
 */
public class PoliticaDeReintento {

    public static final int INTENTOS_MAXIMOS = 3;

    /** {@code true} si, después de {@code intentosHechos} intentos fallidos por {@code motivo}, hay que reintentar. */
    public boolean debeReintentar(int intentosHechos, MotivoFallo motivo) {
        boolean transitoria = motivo == MotivoFallo.CONEXION || motivo == MotivoFallo.DESCONOCIDO;
        return transitoria && intentosHechos < INTENTOS_MAXIMOS;
    }
}
