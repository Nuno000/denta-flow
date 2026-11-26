package ar.dentaflow.dominio.turnos;

/** Genera el token de acceso del turno: aleatorio, impredecible y apto para una URL (design D9). */
public interface GeneradorDeTokens {

    String nuevoToken();
}
