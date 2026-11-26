package ar.dentaflow.infraestructura.agenda;

import ar.dentaflow.aplicacion.agenda.ReglasDeConsultaDeHuecos;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** Configuración de grilla y rango máximo del motor de disponibilidad (design D8). */
@Configuration
@ConfigurationProperties(prefix = "app.disponibilidad")
@Validated
public class PropiedadesDeDisponibilidad {

    @Min(5)
    @Max(60)
    private int pasoMinutos = 15;

    @Min(1)
    @Max(31)
    private int diasMaximos = 14;

    public int getPasoMinutos() {
        return pasoMinutos;
    }

    public void setPasoMinutos(int pasoMinutos) {
        this.pasoMinutos = pasoMinutos;
    }

    public int getDiasMaximos() {
        return diasMaximos;
    }

    public void setDiasMaximos(int diasMaximos) {
        this.diasMaximos = diasMaximos;
    }

    @Bean
    public ReglasDeConsultaDeHuecos reglasDeConsultaDeHuecos() {
        return new ReglasDeConsultaDeHuecos(pasoMinutos, diasMaximos);
    }
}
