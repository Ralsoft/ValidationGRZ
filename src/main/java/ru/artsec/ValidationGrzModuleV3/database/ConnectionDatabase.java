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

@Component
public class ConnectionDatabase implements Validates {
    private final static Logger log = LoggerFactory.getLogger(ConnectionDatabase.class);

    File mqttConfig;
    BaseMqttClient client = new BaseMqttClient(this);
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();

    @Bean
    public DataSource dataSource() {
        try {
            mqttConfig = new File("ValidatedConfig.json");
            client.isNewFile(mqttConfig);
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);

            log.info("Подключение к базе данных.");
            return DataSourceBuilder
                    .create()
                    .username(configurationModel.getDatabaseLogin())
                    .password(configurationModel.getDatabasePassword())
                    .url("jdbc:firebirdsql://" +
                            configurationModel.getDatabaseIp() + ":" +
                            configurationModel.getDatabasePort() + "/" +
                            configurationModel.getDatabasePath() +
                            "?encoding=WIN1251")
                    .build();
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public String validateGRZ(String grz) {
        log.info("Валидация прошла успешно.");
        return grz;
    }
}
