package ru.artsec.ValidationGrzModuleV3.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.artsec.ValidationGrzModuleV3.database.ConnectionDatabase;
import ru.artsec.ValidationGrzModuleV3.model.ConfigurationModel;
import ru.artsec.ValidationGrzModuleV3.model.Door;
import ru.artsec.ValidationGrzModuleV3.model.Message;
import ru.artsec.ValidationGrzModuleV3.model.Monitor;
import ru.artsec.ValidationGrzModuleV3.service.MqttService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;

import java.util.Map;

@Component
public class BaseMqttClient implements MqttService {
    private final static Logger log = LoggerFactory.getLogger(BaseMqttClient.class);
    File mqttConfig;
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();
    String eventType;
    String idPep;
    ConnectionDatabase connectionDB = new ConnectionDatabase();
    public BaseMqttClient() throws SQLException, IOException {
    }

    @Override
    public void getConnection() throws InterruptedException {
        mqttConfig = new File("ValidatedConfig.json");
        isNewFile(mqttConfig);
        try {
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try(MqttClient mqttClient = new MqttClient(
                "tcp://" + configurationModel.getMqttClientIp() + ":" +
                configurationModel.getMqttClientPort(),
                InetAddress.getLocalHost() + "-Validation"
                    )) {

            log.info(
                    "Создание подключения клиента... HOST_NAME = " + configurationModel.getMqttClientIp() +
                    ", PORT = " + configurationModel.getMqttClientPort() +
                    ", USERNAME = " + configurationModel.getMqttUsername() +
                    ", PASSWORD = " + configurationModel.getMqttPassword()
                    );


            mqttClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    try {
                        log.info("Выполнение подписки на топик... ТОПИК: Parking/IntegratorCVS");
                        mqttClient.subscribe("Parking/IntegratorCVS", ((topic, message) -> {
                            try{
                                log.info("Получено сообщение! ТОПИК: " + topic + " СООБЩЕНИЕ: " + message);

                                ObjectMapper mapper = new ObjectMapper();
                                Map map = mapper.readValue(message.toString(), Map.class);

                                String grz = map.get("grz").toString();
                                int camNumber = Integer.parseInt(map.get("camNumber").toString());

                                eventType = null; // Обновляем значение, чтобы не кэшировалось
                                execute(grz,camNumber);
//                                publishResultProcedure(camNumber,eventType,grz);

                                try {
                                    ObjectMapper mapperMsg = new ObjectMapper();
                                    Monitor monitor = new Monitor();
                                    Door door = new Door(camNumber);

                                    var listMessagesBuffer = new ArrayList<Message>();
                                    listMessagesBuffer.add(new Message((byte) 0x00, (byte) 0x0, (byte) 0x02, grz));

                                    var listMessages = configurationModel.getStringDictionary().get(eventType);

                                    //логика соединения сообщений из файла конфигурации
                                    if(listMessages != null) {
                                        monitor.setCamNumber(camNumber);
                                        monitor.setMessages(listMessagesBuffer);
                                        listMessagesBuffer.addAll(listMessages);
                                    }

                                    String jsonMonitor = mapperMsg.writeValueAsString(monitor);
                                    String jsonDoor = mapperMsg.writeValueAsString(door);

                                    MqttMessage mqttMessageEventMonitor = new MqttMessage(jsonMonitor.getBytes(StandardCharsets.UTF_8));
                                    MqttMessage mqttMessageEventDoor = new MqttMessage(jsonDoor.getBytes(StandardCharsets.UTF_8));
                                    MqttMessage mqttEventType = new MqttMessage(eventType.getBytes());
                                    MqttMessage mqttGRZ = new MqttMessage(grz.getBytes());

                                    switch (eventType) {
                                        case "46", "65" -> {
                                            mqttClient.publish("Parking/MonitorDoor/Monitor/View", mqttMessageEventMonitor);
                                            mqttClient.publish("Parking/Validation/Result/NotAcceptGRZ", mqttGRZ);
                                            mqttClient.publish("Parking/Validation/Result/EventType", mqttEventType);

                                            log.info("Сообщение: \"" + mqttMessageEventMonitor + "\" успешно отправлено. На топик Parking/MonitorDoor/Monitor/View");
                                            log.info("Сообщение: \"" + mqttEventType + "\" успешно отправлено. На топик Parking/ResultEventType/");
                                            log.info("Сообщение: \"" + mqttGRZ + "\" успешно отправлено. На топик Parking/Validation/Result/NotAcceptGRZ");
                                        }
                                        case "50" -> {
                                            mqttClient.publish("Parking/MonitorDoor/Monitor/View", mqttMessageEventMonitor);
                                            mqttClient.publish("Parking/Validation/Result/AcceptGRZ", mqttGRZ);
                                            mqttClient.publish("Parking/MonitorDoor/Door/Open", mqttMessageEventDoor);
                                            mqttClient.publish("Parking/Validation/Result/EventType", mqttEventType);

                                            log.info("Сообщение: \"" + mqttMessageEventMonitor + "\" успешно отправлено. На топик Parking/MonitorDoor/Monitor/View");
                                            log.info("Сообщение: \"" + mqttGRZ + "\" успешно отправлено. Parking/Validation/Result/AcceptGRZ");
                                            log.info("Сообщение: \"" + mqttMessageEventDoor + "\" успешно отправлено. На топик Parking/MonitorDoor/Door/Open");
                                            log.info("Сообщение: \"" + mqttEventType + "\" успешно отправлено. На топик Parking/ResultEventType/");
                                        }
                                        default -> {
                                            log.warn("Неизвестный EVENT_TYPE = " + eventType);
                                        }
                                    }

                                } catch (Exception ex) {
                                    log.error("publishResultProcedure Ошибка: " + ex);
                                }
                            } catch (Exception ex) {
                                log.error("messageHandling Ошибка: " + ex);
                            }
                        }));
                        log.info("Подписка на топик Parking/IntegratorCVS произошла успешно.");
                    } catch (Exception ex) {
                        log.error("getSubscribe Ошибка: " + ex);
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("Соединение с MQTT потеряно.");
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
//                    System.out.println("11111111111");
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(5000);
            options.setMaxInflight(20);
            options.setUserName(configurationModel.getMqttUsername());
            options.setPassword(configurationModel.getMqttPassword().toCharArray());
            log.info(
                    "Выставленные настройки MQTT: " +
                    "Автоматический реконнект = " + options.isAutomaticReconnect()
                    );
            mqttClient.connect(options);
            log.info("Успешное поключение клиента - " + mqttClient.getServerURI());
        } catch (Exception e) {
            log.error("getConnection Ошибка: " + e);
        }
    }


    public void execute(String grz,int camNumber) {
        try {
            var idDev = configurationModel.getCameraIdDeviceIdDictionary().get(camNumber);

            Connection connection = connectionDB.connected();

            log.info("Входящие параметры для процедуры: " +
                    "ID_DEV: " + idDev +
                    " ID_CARD: " + grz +
                    " GRZ: " + grz
            );

            log.info("Информация о подключении к базе данных. " +
                    "LOGIN: " + configurationModel.getDatabaseLogin() + ", " +
                    "PASSWORD: " + configurationModel.getDatabasePassword() + ", " +
                    "IP: " + configurationModel.getDatabaseIp() + ", " +
                    "PORT: " + configurationModel.getDatabasePort() + ", " +
                    "ПУТЬ: " + configurationModel.getDatabasePath());

            log.info("Название подключения к базе данных: " + connection.getMetaData());

            String procedure = "{ call VALIDATEPASS(?,?,?) }";
            CallableStatement call = connection.prepareCall(procedure);

            call.setInt(1, camNumber);
            call.setString(2, grz);
            call.setString(3, grz);

            log.info("Выполнение процедуры... {call VALIDATEPASS("+camNumber+","+grz+","+grz+")}");

            call.executeQuery();

            log.info("Успешное выполнение процедуры.");

            call.closeOnCompletion();

            eventType = call.getString(1);
            idPep = call.getString(2);

            log.info("Получена информация из процедуры. " +
                    "EVENT_TYPE: " + eventType +
                    " IP_PEP: " + idPep
            );

        } catch (Exception ex) {
            log.error("execute Ошибка: " + ex);
        }
    }

    public void isNewFile(File file) {
        try {
            if (file.createNewFile()) {

                FileOutputStream out = new FileOutputStream(file);

                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                String json = ow.writeValueAsString(new ConfigurationModel());

                out.write(json.getBytes());
                out.close();

                log.info("Файл конфигурации успешно создан. Запустите программу заново.  ПУТЬ: " + file.getAbsolutePath());
                System.exit(0);
            }
        } catch (IOException e) {
            log.error("isNewFile Ошибка: " + e);
        }
    }

}
