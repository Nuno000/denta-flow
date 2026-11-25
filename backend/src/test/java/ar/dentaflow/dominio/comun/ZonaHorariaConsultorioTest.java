package ar.dentaflow.dominio.comun;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Spec zona-horaria-consultorio. Java puro, sin contexto de Spring. */
class ZonaHorariaConsultorioTest {

    private TimeZone zonaOriginal;

    @BeforeEach
    void servidorEnUtc() {
        zonaOriginal = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void restaurarZona() {
        TimeZone.setDefault(zonaOriginal);
    }

    @Test
    void laZonaEsLaDelConsultorio() {
        assertThat(ZonaHorariaConsultorio.ZONA).isEqualTo(ZoneId.of("America/Argentina/Buenos_Aires"));
    }

    @Test
    void horaLocalDelConsultorioAInstanteConServidorEnUtc() {
        Instant instante = ZonaHorariaConsultorio.aInstante(LocalDateTime.of(2026, 12, 1, 9, 0));

        assertThat(instante).isEqualTo(Instant.parse("2026-12-01T12:00:00Z"));
    }

    @Test
    void instanteAHoraDelConsultorioConCambioDeDia() {
        LocalDateTime horaLocal = ZonaHorariaConsultorio.aHoraLocal(Instant.parse("2026-12-01T02:30:00Z"));

        assertThat(horaLocal).isEqualTo(LocalDateTime.of(2026, 11, 30, 23, 30));
    }

    @Test
    void hoyYAhoraConRelojFijo() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), ZoneOffset.UTC);

        assertThat(ZonaHorariaConsultorio.hoy(reloj)).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(ZonaHorariaConsultorio.ahora(reloj)).isEqualTo(LocalDateTime.of(2026, 11, 26, 12, 0));
    }

    @Test
    void hoyCambiaDeDiaSegunLaZonaDelConsultorio() {
        Clock reloj = Clock.fixed(Instant.parse("2026-11-27T02:00:00Z"), ZoneOffset.UTC);

        assertThat(ZonaHorariaConsultorio.hoy(reloj)).isEqualTo(LocalDate.of(2026, 11, 26));
    }
}
