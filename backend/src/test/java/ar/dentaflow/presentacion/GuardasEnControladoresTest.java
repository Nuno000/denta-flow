package ar.dentaflow.presentacion;

import static org.assertj.core.api.Assertions.assertThat;

import ar.dentaflow.infraestructura.seguridad.guardas.SoloAdministrador;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spec autorizacion-por-rol — "Rol verificado en el backend". Todo endpoint de un controlador de
 * {@code presentacion} declara una guarda de rol (design D8), salvo el login y los controladores de
 * {@code presentacion.publico} (reserva online). Así un endpoint nuevo sin guarda falla en CI.
 */
class GuardasEnControladoresTest {

    /** Endpoints públicos a propósito: {@code Clase#metodo}. */
    private static final Set<String> PUBLICOS = Set.of("AuthController#ingresar");

    @Test
    void todoEndpointDeclaraUnaGuardaDeRol() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider escaner = new ClassPathScanningCandidateComponentProvider(false);
        escaner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));

        List<String> sinGuarda = new ArrayList<>();
        int controladores = 0;
        for (BeanDefinition definicion : escaner.findCandidateComponents("ar.dentaflow.presentacion")) {
            Class<?> controlador = Class.forName(definicion.getBeanClassName());
            if (controlador.isMemberClass() || controlador.getPackageName().startsWith("ar.dentaflow.presentacion.publico")) {
                continue; // controladores de prueba anidados en tests, o rutas públicas de la reserva
            }
            controladores++;
            sinGuarda.addAll(endpointsSinGuarda(controlador));
        }

        assertThat(controladores).as("controladores revisados").isGreaterThanOrEqualTo(3);
        assertThat(sinGuarda).as("endpoints sin guarda de rol").isEmpty();
    }

    @RestController
    static class ControladorSinGuarda {

        @GetMapping("/x")
        void abierto() {
        }

        @SoloAdministrador
        @GetMapping("/y")
        void protegido() {
        }
    }

    @Test
    void elDetectorMarcaUnEndpointSinGuarda() {
        assertThat(endpointsSinGuarda(ControladorSinGuarda.class)).containsExactly("ControladorSinGuarda#abierto");
    }

    static List<String> endpointsSinGuarda(Class<?> controlador) {
        boolean guardaEnLaClase = tieneGuarda(controlador);
        return Arrays.stream(controlador.getDeclaredMethods())
                .filter(metodo -> AnnotatedElementUtils.hasAnnotation(metodo, RequestMapping.class))
                .map(metodo -> controlador.getSimpleName() + "#" + metodo.getName())
                .filter(nombre -> !PUBLICOS.contains(nombre))
                .filter(nombre -> !guardaEnLaClase && !tieneGuarda(metodo(controlador, nombre)))
                .sorted()
                .toList();
    }

    private static Method metodo(Class<?> controlador, String nombre) {
        String soloMetodo = nombre.substring(nombre.indexOf('#') + 1);
        return Arrays.stream(controlador.getDeclaredMethods())
                .filter(m -> m.getName().equals(soloMetodo))
                .findFirst()
                .orElseThrow();
    }

    private static boolean tieneGuarda(AnnotatedElement elemento) {
        return AnnotatedElementUtils.hasAnnotation(elemento, PreAuthorize.class);
    }
}
