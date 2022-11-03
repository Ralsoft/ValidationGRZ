package ru.artsec.ValidationGrzModuleV3.EventTypeList;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class EventType {
    Map<String, String> event = new HashMap<>();

    public MqttMessage resultEvent(String eventNum) {
        addEvent();
        return new MqttMessage(event.get(eventNum).getBytes());
    }

    void addEvent() {
        event.put("46", "Неизвестная карточка.");
        event.put("50", "Действительная карточка.");
        event.put("65", "Недействительная карточка.");
    }
}
