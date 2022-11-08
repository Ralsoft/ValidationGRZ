package ru.artsec.ValidationGrzModuleV3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import ru.artsec.ValidationGrzModuleV3.service.ValidationServiceImpl;

@SpringBootApplication
public class ValidationGRZApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ValidationGRZApplication.class);

    final ApplicationContext applicationContext;

    public ValidationGRZApplication(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public static void main(String[] args) {
        log.info("Начало работы программы.");
        SpringApplication.run(ValidationGRZApplication.class, args);
    }


    @Override
    public void run(String... args) {
        try {
            ValidationServiceImpl validationServiceImpl = applicationContext.getBean("validationServiceImpl", ValidationServiceImpl.class);
            validationServiceImpl.getConnectionMqttClient();
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
    }
}
