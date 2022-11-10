package ru.artsec.ValidationGrzModuleV3.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import ru.artsec.ValidationGrzModuleV3.model.ConfigurationModel;
import ru.artsec.ValidationGrzModuleV3.mqtt.BaseMqttClient;
import ru.artsec.ValidationGrzModuleV3.validate.Validates;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class ConnectionDatabase implements Validates {
    private final static Logger log = LoggerFactory.getLogger(ConnectionDatabase.class);

    File mqttConfig;
    BaseMqttClient client = new BaseMqttClient(this);
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();
    DataSource source;
    int count;

    @Bean
    DataSource getConnection() throws IOException {
        try {
            mqttConfig = new File("ValidatedConfig.json");
            client.isNewFile(mqttConfig);
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);
            isConnected();
            source = DataSourceBuilder.create()
                    .username(configurationModel.getDatabaseLogin())
                    .password(configurationModel.getDatabasePassword())
                    .url("jdbc:firebirdsql://"
                            + configurationModel.getDatabaseIp()
                            + ":"
                            + configurationModel.getDatabasePort()
                            + "/"
                            + configurationModel.getDatabasePath()
                            + "?encoding=WIN1251")
                    .build();
            return source;
        } catch (
                Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
        return null;
    }

    void isConnected() throws InterruptedException {
        log.info("Подключение к базе данных. Попытка: " + ++count);
        try (Connection ignored = DriverManager.getConnection("jdbc:firebirdsql://" + configurationModel.getDatabaseIp() + ":" + configurationModel.getDatabasePort() + "/" + configurationModel.getDatabasePath() + "?encoding=WIN1251", configurationModel.getDatabaseLogin(), configurationModel.getDatabasePassword())) {
            if (!ignored.isClosed()) {
                log.info("Подключение к базе данных произошло успешно.");
            }
        } catch (Exception ex) {
            Thread.sleep(10000);
            log.error("Ошибка: " + ex.getMessage());
            isConnected();
        }
    }

    @Override
    public String validateGRZ(String grz) {
        log.info("Валидация прошла успешно.");
        return grz;
    }
}
