package ar.dentaflow.dominio.recursos;

import java.lang.reflect.Field;

/** Ayudante de pruebas: asigna el id como lo haría la base, sin agregar setters públicos al dominio. */
public final class AyudanteDeIds {

    private AyudanteDeIds() {
    }

    public static <T> T conId(T entidad, Long id) {
        try {
            Field campo = entidad.getClass().getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(entidad, id);
            return entidad;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
