package cl.joaquin.siga.Config;

import cl.joaquin.siga.Services.SeedDataService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// Al arrancar carga el Apéndice A si la base está vacía. Se apaga con siga.seed.enabled=false
// (los tests lo hacen en src/test/resources/config)
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "siga.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SeedDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataRunner.class);

    private final SeedDataService seedDataService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            seedDataService.seedIfEmpty();
        } catch (RuntimeException e) {
            // las 3 réplicas arrancan a la vez: si otra cargó primero, la transacción de esta se revierte
            // por las unique y no debe tumbar el arranque
            log.warn("No se aplicó la carga inicial (probablemente otra réplica la hizo): {}", e.getMessage());
        }
    }
}
