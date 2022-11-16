package ru.artsec.ValidationGrzModuleV3.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jdk.jfr.Experimental;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
    File mqttConfig;
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();
//    ConnectionDatabase connectDatabase = new ConnectionDatabase();
    String eventType;
    String idPep;
    Connection connection;
    public BaseMqttClient() throws SQLException, IOException {
    }

    @Override
    public void getConnection() throws InterruptedException {
        try {
            mqttConfig = new File("ValidatedConfig.json");
            isNewFile(mqttConfig);
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);

            log.info(
                    "Создание подключения клиента... HOST_NAME = " + configurationModel.getMqttClientIp() +
                    ", PORT = " + configurationModel.getMqttClientPort() +
                    ", USERNAME = " + configurationModel.getMqttUsername() +
                    ", PASSWORD = " + configurationModel.getMqttPassword()
                    );
            mqttClient = new MqttClient(
                    "tcp://" + configurationModel.getMqttClientIp() + ":" +
                    configurationModel.getMqttClientPort(),
                    InetAddress.getLocalHost() + "-Validation"
                    );
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setUserName(configurationModel.getMqttUsername());
            options.setPassword(configurationModel.getMqttPassword().toCharArray());
            log.info(
                    "Выставленные настройки MQTT: " +
                    "Автоматический реконнект = " + options.isAutomaticReconnect()
                    );
//            mqttClient.setCallback(new MqttCallback() {
//                @Override
//                public void connectionLost(Throwable throwable) {
//                    log.error("Ошибка: " + throwable.getMessage());
//                    if (!mqttClient.isConnected()) {
//                        try {
//                            log.info("Переподключение... 10 сек");
//                            Thread.sleep(10000);
//                            getConnection();
//                        } catch (InterruptedException e) {
//                            log.error("getConnection Ошибка: " + e);
//                        }
//                    }
//                }
//
//                @Override
//                public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
//                    log.info("Получено сообщение: " + s);
//                }
//
//                @Override
//                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
////                    log.info("deliveryComplete " + iMqttDeliveryToken.toString());
//                }
//            });
            mqttClient.connect(options);
            getSubscribe();
            log.info("Успешное поключение клиента - " + mqttClient.getServerURI());
        } catch (Exception e) {
            log.error("getConnection Ошибка: " + e);
            if (!mqttClient.isConnected()){
                log.info("Переподключение... 5 сек");
                Thread.sleep(5000);
                getConnection();
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
            log.error("isNewFile Ошибка: " + e);
        }
    }

    @Override
    public void getSubscribe() {
        try {
            log.info("Выполнение подписки на топик... ТОПИК: Parking/IntegratorCVS");
            mqttClient.subscribe("Parking/IntegratorCVS", this::messageHandling);
            log.info("Подписка на топик Parking/IntegratorCVS произошла успешно.");
        } catch (Exception ex) {
            log.error("getSubscribe Ошибка: " + ex);
        }
    }

    private synchronized void messageHandling(String topic, MqttMessage message) {
        try{
            log.info("Получено сообщение! ТОПИК: " + topic + " СООБЩЕНИЕ: " + message);

            ObjectMapper mapper = new ObjectMapper();
            Map map = mapper.readValue(message.toString(), Map.class);

            String grz = map.get("grz").toString();
            int camNumber = Integer.parseInt(map.get("camNumber").toString());

            implementQueryProcedure(grz, camNumber);
        } catch (Exception ex) {
            log.error("messageHandling Ошибка: " + ex);
        }
    }

    @Override
    public void implementQueryProcedure(String grz, int camNumber) { // Вызов процедуры validatepass
        try {
            var idDev = configurationModel.getCameraIdDeviceIdDictionary().get(camNumber);

            log.info("Входящие параметры для процедуры: " +
                    "ID_DEV: " + idDev +
                    " ID_CARD: " + grz +
                    " GRZ: " + grz
                    );
            eventType = null; // Обновляем значение, чтобы не кэшировалось
            execute(grz, camNumber);

            publishResultProcedure(camNumber, eventType, grz);
        } catch (Exception ex) {
            log.error("implementQueryProcedure Ошибка: " + ex);
        }
    }

    @Override
    public void publishResultProcedure(int camNumber, String eventType, String grz) throws InterruptedException {
        try {
            ObjectMapper mapper = new ObjectMapper();
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
            if(!mqttClient.isConnected()){
                getConnection();
            }
        }
    }


    public void execute(String grz,int camNumber) throws InterruptedException, SQLException {
        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:firebirdsql://localhost:3050/C:/Users/Sergey/Desktop/DB/TEST.FDB?encoding=WIN1251",
                    "SYSDBA", "masterkey");

//            connection = connectDatabase.connected();

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

            connection.close();
        } catch (Exception ex) {
            log.error("execute Ошибка: " + ex);
//            log.info("Переподключение... 5 сек");
//            Thread.sleep(5000);
//            if(connection.isClosed()) {
////                connection = connectDatabase.connected();
//                execute(grz, camNumber);
            }
        }


    @Override
    public void startBackgroundMethods(){
        new Thread(()->{ // проверка подключения к mqtt
            while (true){
                String str = "publish";
                try {
                    log.info("Подключение к mqtt: " +(
                            mqttClient.isConnected()  ? "присутствует" : "отсутствует"));
                    Thread.sleep(15000);
                } catch (Exception e) {
                    log.error("startBackgroundMethods Ошибка: " + e);
                }
            }
        }).start();
    }
}
