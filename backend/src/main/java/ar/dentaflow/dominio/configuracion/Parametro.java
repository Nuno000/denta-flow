package ar.dentaflow.dominio.configuracion;

import ar.dentaflow.dominio.comun.ValidacionDeDominioException;
import java.time.Instant;

/**
 * Parámetro de configuración del consultorio (design D3). Las filas las crea la migración con un valor
 * inicial provisorio; el administrador solo las actualiza. Al guardar un valor válido deja de ser
 * provisorio, aunque sea el mismo valor.
 */
public class Parametro {

    private String clave;
    private String valor;
    private boolean provisorio;
    private Instant actualizadoEn;
    private Long actualizadoPor;

    protected Parametro() {
    }

    /**
     * Valida y guarda el valor nuevo con su autor e instante, y lo confirma. Si el valor es inválido
     * lanza {@link ValidacionDeDominioException} sin cambiar nada.
     */
    public void actualizar(String valorNuevo, Long autorId, Instant ahora) {
        String normalizado = clave().normalizar(valorNuevo);
        this.valor = normalizado;
        this.provisorio = false;
        this.actualizadoPor = autorId;
        this.actualizadoEn = ahora;
    }

    /** Valor de un parámetro entero; la conversión a unidades de tiempo la hace el servicio. */
    public int valorEntero() {
        if (clave().tipo() != TipoParametro.ENTERO) {
            throw new IllegalStateException("El parámetro " + clave + " no es un entero");
        }
        return Integer.parseInt(valor);
    }

    public ClaveParametro clave() {
        return ClaveParametro.desdeCodigo(clave);
    }

    public String valor() {
        return valor;
    }

    public boolean isProvisorio() {
        return provisorio;
    }

    public Instant actualizadoEn() {
        return actualizadoEn;
    }

    public Long actualizadoPor() {
        return actualizadoPor;
    }

    @Override
    public boolean equals(Object otro) {
        return this == otro || otro instanceof Parametro that && clave != null && clave.equals(that.clave);
    }

    @Override
    public int hashCode() {
        return clave == null ? 0 : clave.hashCode();
    }
}
