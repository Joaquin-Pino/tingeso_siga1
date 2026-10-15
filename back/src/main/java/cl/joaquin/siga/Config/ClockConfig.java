package cl.joaquin.siga.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    // los contenedores corren en UTC, pero "hoy" para el instituto es la fecha de Santiago
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Santiago"));
    }
}
