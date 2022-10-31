package ru.artsec.ValidationGrzModuleV3.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.artsec.ValidationGrzModuleV3.model.Message;
import ru.artsec.ValidationGrzModuleV3.model.Monitor;
import ru.artsec.ValidationGrzModuleV3.model.ValidationInfo;
import ru.artsec.ValidationGrzModuleV3.service.MqttService;
import ru.artsec.ValidationGrzModuleV3.validate.Validates;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class BaseMqttClient implements MqttService {
    private final static Logger log = LoggerFactory.getLogger(BaseMqttClient.class);
    @PersistenceContext
    EntityManager em;
    static IMqttClient iMqttClient = null;
    MqttConnectOptions options;
    @Value("${clients.url}")
    private String URL;
    @Value("${clients.id}")
    private String clientId;
    @Value("${clients.topic1}")
    private String topic1;
    final Validates validates;
    JsonNode rootNode;

    public BaseMqttClient(Validates validates) {
        this.validates = validates;
    }

    @Override
    public void getConnection() {
        try {
            log.info("Попытка подключения клиента. ID = " + clientId + " URL = " + URL);

            iMqttClient = new MqttClient("tcp://" + URL, clientId);
            options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            iMqttClient.connect(options);

            log.info("Успешное поключение клиента - " + clientId);
        } catch (Exception e) {
            log.error("Ошибка: " + e.getMessage());
        }
    }

    @Override
    public void getSubscribe() {
        try {
            log.info("Попытка подписки на топик. ТОПИК: " + topic1);
            iMqttClient.subscribe(topic1, (topic, message) -> {
                try {
                    log.info("Получено сообщение от топика. ТОПИК \"" + topic + "\" СООБЩЕНИЕ: \"" + message + "\"");

                    ObjectMapper mapper = new ObjectMapper();
                    Map map = mapper.readValue(message.toString(), Map.class);

                    String grz = map.get("grz").toString();
                    String camNumber = map.get("camNumber").toString();

                    implementQueryProcedure(grz, camNumber);
                } catch (Exception ex) {
                    log.error("Ошибка: " + ex.getMessage());
                }
            });
            log.info("Подписка на топик " + topic1 + " произошла успешно.");
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
    }

    @Override
    public void implementQueryProcedure(String grz, String camNumber) {
        try {
            log.info("Выполняется валидация ГРЗ.");

            String validGRZ = validates.validateGRZ(grz);

            log.info("Выполнение процедуры...");

            StoredProcedureQuery storedProcedureQuery = em.createNamedStoredProcedureQuery("validatepass").
                    setParameter("ID_DEV", 279).
                    setParameter("ID_CARD", validGRZ).
                    setParameter("GRZ", validGRZ);
            var eventType = (String) storedProcedureQuery.getOutputParameterValue("EVENT_TYPE");
            var idPep = (String) storedProcedureQuery.getOutputParameterValue("ID_PEP");

            log.info("Получены аргументы: EVENT_TYPE = " + eventType + " ID_PEP = " + idPep);

            ObjectMapper mapper = new ObjectMapper();
            Monitor monitor = new Monitor();

            monitor.setIp("localhost");
            monitor.setPort(1212);
            monitor.setCamNumber("1");
            monitor.setMessages(new Message((byte) 0x00, (byte) 0x00, (byte) 0x00, "Привет"));

            String jsonMonitor = mapper.writeValueAsString(monitor);


            ValidationInfo info = new ValidationInfo(validGRZ, "2");
            String jsonValidationInfo = mapper.writeValueAsString(info);

            MqttMessage mqttMessageEventMonitor = new MqttMessage(jsonMonitor.getBytes(StandardCharsets.UTF_8));
            MqttMessage mqttMessageEventValidationInfo = new MqttMessage(jsonValidationInfo.getBytes(StandardCharsets.UTF_8));
            MqttMessage mqttEventType = new MqttMessage(eventType.getBytes());
            MqttMessage mqttGRZValid = new MqttMessage(validGRZ.getBytes());

            iMqttClient.publish("Parking/Validation/Result", mqttEventType);
            iMqttClient.publish("Parking/Validation/GRZ_Valid", mqttGRZValid);
            iMqttClient.publish("Parking/Validation/", mqttMessageEventValidationInfo);
            iMqttClient.publish("Parking/MonitorDoor/Monitor/View/", mqttMessageEventMonitor);


            log.info("Сообщение: \"" + mqttMessageEventMonitor + "\" успешно отправлено. На топик Parking/MonitorDoor/Monitor/View/");
            log.info("Сообщение: \"" + mqttMessageEventValidationInfo + "\" успешно отправлено. На топик Parking/Validation/");
            log.info("Сообщение: \"" + mqttEventType + "\" успешно отправлено. На топик Parking/ResultEventType/");
        } catch (Exception ex) {
            log.error("Ошибка: " + ex.getMessage());
        }
    }
}
