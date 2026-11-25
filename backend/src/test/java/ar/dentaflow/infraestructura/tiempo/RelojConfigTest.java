package ar.dentaflow.infraestructura.tiempo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ar.dentaflow.dominio.comun.ZonaHorariaConsultorio;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/** Spec zona-horaria-consultorio — "Hora actual controlable": una única fuente de "ahora". */
class RelojConfigTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    class RelojDelSistema {

        @Autowired
        private Clock reloj;

        @Test
        void elRelojDeLaAplicacionEsElDelSistemaEnUtc() {
            assertThat(reloj.getZone()).isEqualTo(ZoneOffset.UTC);
            assertThat(reloj.instant()).isCloseTo(Instant.now(), within(5, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    class RelojFijoEnPruebas {

        @TestConfiguration
        static class RelojFijo {
            @Bean
            @Primary
            Clock relojFijo() {
                return Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), ZoneOffset.UTC);
            }
        }

        @Autowired
        private Clock reloj;

        @Test
        void unaPruebaPuedeFijarLaHoraActual() {
            assertThat(ZonaHorariaConsultorio.ahora(reloj)).isEqualTo(LocalDateTime.of(2026, 11, 26, 12, 0));
        }
    }
}
