package ru.artsec.ValidationGrzModuleV3.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.artsec.ValidationGrzModuleV3.model.ConfigurationModel;
import ru.artsec.ValidationGrzModuleV3.validate.Validates;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionDatabase implements Validates {
    private final static Logger log = LoggerFactory.getLogger(ConnectionDatabase.class);

    File mqttConfig;
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();
    DataSource source;
    int count;

    public ConnectionDatabase() throws SQLException {
    }

    public Connection getConnectionDB() {
        return connectionDB;
    }

    Connection connectionDB;

    public Connection isConnected() throws InterruptedException {

        log.info("Подключение к базе данных. Попытка: " + ++count);

        try {
            mqttConfig = new File("ValidatedConfig.json");
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);
            log.info("Информация о подключении к базе данных. " +
                    "LOGIN: " + configurationModel.getDatabaseLogin() + ", " +
                    "PASSWORD: " + configurationModel.getDatabasePassword() + ", " +
                    "IP: " + configurationModel.getDatabaseIp() + ", " +
                    "PORT: " + configurationModel.getDatabasePort() + ", " +
                    "ПУТЬ: " + configurationModel.getDatabasePath()
            );
            connectionDB = DriverManager.getConnection("jdbc:firebirdsql://" + configurationModel.getDatabaseIp() + ":" + configurationModel.getDatabasePort() + "/" + configurationModel.getDatabasePath() + "?encoding=WIN1251&autoReconnect=true", configurationModel.getDatabaseLogin(), configurationModel.getDatabasePassword());

            if (!connectionDB.isClosed())
                log.info("Подключение к базе данных произошло успешно.");

            return connectionDB;
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
            Thread.sleep(10000);
            isConnected();
        }
        return null;
    }


    @Override
    public String validateGRZ(String grz) {
        return grz;
    }

}
