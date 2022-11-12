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

//@Component
public class ConnectionDatabase implements Validates {
    private final static Logger log = LoggerFactory.getLogger(ConnectionDatabase.class);

    File mqttConfig;
    //    BaseMqttClient client = new BaseMqttClient(this);
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();
    DataSource source;
    int count;
//    HikariDataSource dataSource;

    public ConnectionDatabase() throws SQLException {
    }

//    @Bean
//    public HikariDataSource getConnectionDB() {
//        try {
//            dataSource = new HikariDataSource();
//            mqttConfig = new File("ValidatedConfig.json");
//            client.isNewFile(mqttConfig);
//            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);
//            log.info("Попытка подключения к базе данных. " + "ЛОГИН: " + configurationModel.getDatabaseLogin() + ", " + "ПУТЬ:" + configurationModel.getDatabasePath() + ", " + "IP: " + configurationModel.getDatabaseIp() + ", " + "PORT: " + configurationModel.getDatabasePort());
//            isConnected();
//            dataSource.setUsername("SYSDBA");
//            dataSource.setPassword("masterkey");
//            dataSource.setJdbcUrl("jdbc:firebirdsql://" + configurationModel.getDatabaseIp() + ":" + configurationModel.getDatabasePort() + "/" + configurationModel.getDatabasePath() + "?encoding=WIN1251&autoReconnect=true");
//            dataSource.setConnectionTimeout(3000);
////            dataSource.setMaximumPoolSize(999);
//            return dataSource;
//        } catch (Exception ex) {
//            log.error("Ошибка: " + ex.getMessage());
//        }
//        return null;
//    }

    public Connection getConnectionDB() {
        return connectionDB;
    }

    Connection connectionDB;

    public Connection isConnected() throws InterruptedException {

        log.info("Подключение к базе данных. Попытка: " + ++count);
        try {
            mqttConfig = new File("ValidatedConfig.json");
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);
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
