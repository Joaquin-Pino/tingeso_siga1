package cl.joaquin.siga.Config;

import cl.joaquin.siga.Services.DemoDataService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// Después de la carga del Apéndice A (SeedDataRunner) crea los docentes, estudiantes y la oferta de demo.
// Se apaga con siga.seed.demo.enabled=false (los tests lo hacen en src/test/resources/config)
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "siga.seed.demo.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataRunner.class);

    private final DemoDataService demoDataService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            demoDataService.seedIfEmpty();
        } catch (RuntimeException e) {
            // típicamente Keycloak aún no está listo (las cuentas se crean ahí) o otra réplica cargó primero
            log.warn("No se aplicaron los datos de demo: {}", e.getMessage());
        }
    }
}
