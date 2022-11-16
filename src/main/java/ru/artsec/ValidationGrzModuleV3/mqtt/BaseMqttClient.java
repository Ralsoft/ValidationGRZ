package ru.artsec.ValidationGrzModuleV3.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jdk.jfr.Experimental;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class BaseMqttClient implements MqttService {
    private final static Logger log = LoggerFactory.getLogger(BaseMqttClient.class);
    MqttClient mqttClient;
    MqttConnectOptions options;
    File mqttConfig;
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();
    ConnectionDatabase connectDatabase = new ConnectionDatabase();
    String eventType;
    String idPep;

    public BaseMqttClient() throws SQLException, IOException {
    }

    @Override
    public void getConnection(String name) throws InterruptedException {
        try {
            mqttConfig = new File("ValidatedConfig.json");
            isNewFile(mqttConfig);
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);

            log.info("Попытка подключения клиента. " + ", " +
                    "URL = " + configurationModel.getMqttClientIp() + ":" + configurationModel.getMqttClientPort() + ", " +
                    "ЛОГИН: " + configurationModel.getMqttUsername()
            );

            mqttClient = new MqttClient(
                    "tcp://" + configurationModel.getMqttClientIp() + ":" +
                            configurationModel.getMqttClientPort(), InetAddress.getLocalHost() + "-Validation");
            options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setUserName(configurationModel.getMqttUsername());
            options.setPassword(configurationModel.getMqttPassword().toCharArray());
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    log.error("Соединение потеряно " + throwable.getMessage());
                    if (!mqttClient.isConnected()) {
                        try {
                            Thread.sleep(60000);
                            getConnection(MqttClient.generateClientId());
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                @Override
                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                    log.info("пришло сообщение " + s);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    log.info("deliveryComplete " + iMqttDeliveryToken.toString());
                }
            });
            mqttClient.connect(options);
            getSubscribe();
            log.info("Успешное поключение клиента - " + configurationModel.getMqttClientId());
        } catch (Exception e) {
            log.error("Ошибка: " + e);
            if (!mqttClient.isConnected()){
                Thread.sleep(5000);
                getConnection(name);
            }
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
            log.error("Ошибка: " + e.getMessage());
        }
    }

    @Override
    public void getSubscribe() {
        try {
            log.info("Попытка подписки на топик. ТОПИК: Parking/IntegratorCVS");
            mqttClient.subscribe("Parking/IntegratorCVS", this::messageHandling);
            log.info("Подписка на топик Parking/IntegratorCVS произошла успешно.");
        } catch (Exception ex) {
            log.error("Ошибка: " + ex);
        }
    }

    private synchronized void messageHandling(String topic, MqttMessage message) {
        try{
            log.info("Получено сообщение от топика. ТОПИК \"" + topic + "\" СООБЩЕНИЕ: \"" + message + "\"");

            ObjectMapper mapper = new ObjectMapper();
            Map map = mapper.readValue(message.toString(), Map.class);

            String grz = map.get("grz").toString();
            int camNumber = Integer.parseInt(map.get("camNumber").toString());

            implementQueryProcedure(grz, camNumber);
        } catch (Exception ex) {
            log.error("Ошибка: " + ex);
        }
    }

    @Override
    public void implementQueryProcedure(String grz, int camNumber) { // Вызов процедуры validatepass
        try {
            var idDev = configurationModel.getCameraIdDeviceIdDictionary().get(camNumber);

            log.info("Выполнение процедуры...");
            log.info("Входящие параметры для процедуры: " +
                    "ID_DEV: " + idDev +
                    "ID_CARD: " + grz +
                    " GRZ: " + grz
            );

            execute(grz, camNumber);

            publishResultProcedure(camNumber, eventType, grz);
        } catch (Exception ex) {
            log.error("Ошибка: " + ex);
        }
    }

    @Override
    public void publishResultProcedure(int camNumber, String eventType, String grz) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Monitor monitor = new Monitor();
            Door door = new Door(camNumber);

//            eventType = "47";

            var listMessagesBuffer = new ArrayList<Message>();
            listMessagesBuffer.add(new Message((byte) 0x00, (byte) 0x0, (byte) 0x02, grz));

            var listMessages = configurationModel.getStringDictionary().get(eventType);

            //логика соединения сообщений из файла конфигурации
            if(listMessages != null) {
                monitor.setCamNumber(camNumber);
                monitor.setMessages(listMessagesBuffer);
                listMessagesBuffer.addAll(listMessages);
            }

            String jsonMonitor = mapper.writeValueAsString(monitor);
            String jsonDoor = mapper.writeValueAsString(door);

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
            log.error("Ошибка: " + ex.getMessage());
        }
    }


    public void execute(String grz,int camNumber) {
        try {
            Connection connection = connectDatabase.connected();

            log.info("Название подключения к базе данных: " + connection.getMetaData());

            String procedure = "{ call VALIDATEPASS(?,?,?) }";
            CallableStatement call = connection.prepareCall(procedure);

            call.setInt(1, camNumber);
            call.setString(2, grz);
            call.setString(3, grz);

            call.executeQuery();

            call.closeOnCompletion();

            eventType = call.getString(1);
            idPep = call.getString(2);

            log.info("Соединение с базой данных: " + !connection.isClosed());
        } catch (Exception ex) {
            log.error("Ошибка: " + ex);
        }
    }
}
