package ar.dentaflow.aplicacion.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.dentaflow.dominio.recursos.AyudanteDeIds;
import ar.dentaflow.dominio.usuarios.CodificadorDeCredenciales;
import ar.dentaflow.dominio.usuarios.Rol;
import ar.dentaflow.dominio.usuarios.Usuario;
import ar.dentaflow.dominio.usuarios.UsuarioRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Spec autenticacion-personal — "Ingreso con email y contraseña", "Rechazo genérico de credenciales
 * inválidas" y "Límite de intentos fallidos" (design D6).
 */
class ServicioDeIngresoTest {

    private static final String HASH = "$2a$10$hashDeLaContrasenaCorrecta";
    private static final Instant AHORA = Instant.parse("2026-11-25T13:00:00Z");

    private final UsuarioRepositorio usuarios = mock(UsuarioRepositorio.class);
    private final CodificadorDeCredenciales codificador = mock(CodificadorDeCredenciales.class);
    private final EmisorDeTokens emisor = mock(EmisorDeTokens.class);
    private final LimitadorDeIntentosDeIngreso limitador = new LimitadorDeIntentosDeIngreso(
            Clock.fixed(AHORA, ZoneOffset.UTC), 5, Duration.ofMinutes(15));

    private ServicioDeIngreso servicio;
    private Usuario recepcion;

    @BeforeEach
    void preparar() {
        when(codificador.codificar(anyString())).thenReturn("$2a$10$hashFicticio");
        when(codificador.coincide("correcta", HASH)).thenReturn(true);
        servicio = new ServicioDeIngreso(usuarios, codificador, emisor, limitador);
        recepcion = AyudanteDeIds.conId(Usuario.nuevo("recepcion@example.com", HASH, Rol.RECEPCION, null), 7L);
        when(usuarios.buscarPorEmail("recepcion@example.com")).thenReturn(Optional.of(recepcion));
        when(emisor.emitir(7L)).thenReturn(new TokenEmitido("token-firmado", AHORA.plus(Duration.ofHours(8))));
    }

    @Test
    void conCredencialesCorrectasDevuelveElTokenYElUsuario() {
        ResultadoIngreso resultado = servicio.ingresar("  Recepcion@Example.com ", "correcta");

        assertThat(resultado.token().token()).isEqualTo("token-firmado");
        assertThat(resultado.token().venceEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(resultado.usuario()).isSameAs(recepcion);
    }

    @Test
    void conContrasenaIncorrectaRechazaConElMensajeGenerico() {
        assertThatThrownBy(() -> servicio.ingresar("recepcion@example.com", "incorrecta"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
        verify(emisor, never()).emitir(any());
    }

    @Test
    void conEmailInexistenteIgualComparaContraUnHashFicticioYDaElMismoMensaje() {
        when(usuarios.buscarPorEmail("nadie@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.ingresar("nadie@example.com", "correcta"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
        verify(codificador).coincide(eq("correcta"), eq("$2a$10$hashFicticio"));
    }

    @Test
    void unUsuarioDesactivadoNoIngresaAunqueLaContrasenaSeaCorrecta() {
        recepcion.desactivar();

        assertThatThrownBy(() -> servicio.ingresar("recepcion@example.com", "correcta"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Email o contraseña incorrectos");
        verify(emisor, never()).emitir(any());
    }

    @Test
    void trasCincoFallosRechazaSinConsultarElRepositorioNiEvaluarLaContrasena() {
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> servicio.ingresar("recepcion@example.com", "incorrecta"))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }

        assertThatThrownBy(() -> servicio.ingresar("recepcion@example.com", "correcta"))
                .isInstanceOf(DemasiadosIntentosException.class);
        verify(usuarios, times(5)).buscarPorEmail("recepcion@example.com");
    }

    @Test
    void unIngresoCorrectoReiniciaLosFallos() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> servicio.ingresar("recepcion@example.com", "incorrecta"))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        servicio.ingresar("recepcion@example.com", "correcta");

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> servicio.ingresar("recepcion@example.com", "incorrecta"))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThat(servicio.ingresar("recepcion@example.com", "correcta").usuario()).isSameAs(recepcion);
    }
}
