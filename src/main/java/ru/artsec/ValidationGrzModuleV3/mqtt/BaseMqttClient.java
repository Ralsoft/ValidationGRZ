package ru.artsec.ValidationGrzModuleV3.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.artsec.ValidationGrzModuleV3.model.Door;
import ru.artsec.ValidationGrzModuleV3.model.ConfigurationModel;
import ru.artsec.ValidationGrzModuleV3.model.Message;
import ru.artsec.ValidationGrzModuleV3.model.Monitor;
import ru.artsec.ValidationGrzModuleV3.service.MqttService;
import ru.artsec.ValidationGrzModuleV3.validate.Validates;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Component
public class BaseMqttClient implements MqttService {
    private final static Logger log = LoggerFactory.getLogger(BaseMqttClient.class);
    @PersistenceContext
    EntityManager em;
    static IMqttClient iMqttClient = null;
    MqttConnectOptions options;
    private final Validates validates;
    File mqttConfig;
    ConfigurationModel configurationModel;
    ObjectMapper mapper = new ObjectMapper();

    public BaseMqttClient(@Qualifier("connectionDatabase") Validates validates) {
        this.validates = validates;
    }

    @Override
    public void getConnection() {
        try {
            mqttConfig = new File("ValidatedConfig1.json");
            isNewFile(mqttConfig);
            configurationModel = mapper.readValue(mqttConfig, ConfigurationModel.class);

            log.info("Попытка подключения клиента. ID = " + configurationModel.getMqttClientId() + " URL = " + configurationModel.getMqttClientIp() + ":" + configurationModel.getMqttClientPort());

            iMqttClient = new MqttClient("tcp://" + configurationModel.getMqttClientIp() + ":" + configurationModel.getMqttClientPort(), MqttClient.generateClientId());

            options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(5000);
            iMqttClient.connect(options);

            log.info("Успешное поключение клиента - " + configurationModel.getMqttClientId());
        } catch (Exception e) {
            log.error("Ошибка: " + e.getMessage());
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
            iMqttClient.subscribe("Parking/IntegratorCVS", (topic, message) -> {
                try {
                    log.info("Получено сообщение от топика. ТОПИК \"" + topic + "\" СООБЩЕНИЕ: \"" + message + "\"");

                    ObjectMapper mapper = new ObjectMapper();
                    Map map = mapper.readValue(message.toString(), Map.class);

                    String grz = map.get("grz").toString();
                    int camNumber = Integer.parseInt(map.get("camNumber").toString());

                    implementQueryProcedure(grz, camNumber);
                } catch (Exception ex) {
                    log.error("Ошибка: " + ex.getMessage());
                }
            });
            log.info("Подписка на топик Parking/IntegratorCVS произошла успешно.");
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
    }

    @Override
    public void implementQueryProcedure(String grz, int camNumber) {
        try {
            log.info("Выполняется валидация ГРЗ.");

            String validGRZ = validates.validateGRZ(grz);

            log.info("Выполнение процедуры...");

            var idDev = configurationModel.getCameraIdDeviceIdDictionary().get(camNumber);
            StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery("validatepass").
                    setParameter("ID_DEV", idDev).
                    setParameter("ID_CARD", validGRZ).
                    setParameter("GRZ", validGRZ);
            var eventType = (String) storedProcedureQuery.getOutputParameterValue("EVENT_TYPE");
            var idPep = (String) storedProcedureQuery.getOutputParameterValue("ID_PEP");

            log.info("Получен ID камеры: " + configurationModel.getCameraIdDeviceIdDictionary().get(camNumber));
            log.info("Получены аргументы: EVENT_TYPE = " + eventType + " ID_PEP = " + idPep);

            publushResultProcedure(camNumber, eventType, grz);
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
    }

    @Override
    public void publushResultProcedure(int camNumber, String eventType, String grz) {
        try {

            ObjectMapper mapper = new ObjectMapper();
            Monitor monitor = new Monitor();

            Door door = new Door(camNumber);

            monitor.setCamNumber(camNumber);
            monitor.setMessages(Arrays.asList(new Message((byte) 0x00, (byte) 0x00, (byte) 0x02, grz), new Message((byte) 0x09, (byte) 0x00, (byte) 0x02, eventType)));

            String jsonMonitor = mapper.writeValueAsString(monitor);
            String jsonDoor = mapper.writeValueAsString(door);

            MqttMessage mqttMessageEventMonitor = new MqttMessage(jsonMonitor.getBytes(StandardCharsets.UTF_8));
            MqttMessage mqttMessageEventDoor = new MqttMessage(jsonDoor.getBytes(StandardCharsets.UTF_8));
            MqttMessage mqttEventType = new MqttMessage(eventType.getBytes());
            MqttMessage mqttGRZ = new MqttMessage(grz.getBytes());

            iMqttClient.publish("Parking/MonitorDoor/Monitor/View", mqttMessageEventMonitor);
            switch (eventType) {
                case "46", "65" -> {
                    iMqttClient.publish("Parking/Validation/Result/NotAcceptGRZ", mqttGRZ);
                    iMqttClient.publish("Parking/Validation/Result/EventType", mqttEventType);

                }
                case "50" -> {
                    iMqttClient.publish("Parking/Validation/Result/AcceptGRZ", mqttGRZ);
                    iMqttClient.publish("Parking/MonitorDoor/Door/Open", mqttMessageEventDoor);
                    iMqttClient.publish("Parking/Validation/Result/EventType", mqttEventType);

                    log.info("Сообщение: \"" + mqttGRZ + "\" успешно отправлено. Parking/Validation/Result/AcceptGRZ");
                    log.info("Сообщение: \"" + mqttMessageEventDoor + "\" успешно отправлено. На топик Parking/MonitorDoor/Door/Open");
                }
                default -> {
                    log.warn("Неизвестный EVENT_TYPE = " + eventType);
                }
            }

            log.info("Сообщение: \"" + mqttMessageEventMonitor + "\" успешно отправлено. На топик Parking/MonitorDoor/Monitor/View");
             log.info("Сообщение: \"" + mqttEventType + "\" успешно отправлено. На топик Parking/ResultEventType/");
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
    }
}
