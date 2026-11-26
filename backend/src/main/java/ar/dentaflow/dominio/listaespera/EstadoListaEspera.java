package ar.dentaflow.dominio.listaespera;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;

public enum EstadoListaEspera {
    ACTIVA("activa"),
    OFRECIDA("ofrecida"),
    ATENDIDA("atendida"),
    VENCIDA("vencida");

    private final String codigo;

    EstadoListaEspera(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static EstadoListaEspera desdeCodigo(String codigo) {
        for (EstadoListaEspera estado : values()) {
            if (estado.codigo.equals(codigo)) {
                return estado;
            }
        }
        throw new ValidacionDeDominioException("Estado de lista de espera desconocido: " + codigo);
    }
}
