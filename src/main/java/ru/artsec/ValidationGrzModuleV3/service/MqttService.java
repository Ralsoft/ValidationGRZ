package ru.artsec.ValidationGrzModuleV3.service;

import org.eclipse.paho.client.mqttv3.MqttException;

public interface MqttService {
    void getConnection();

    void getSubscribe() throws MqttException;

    void implementQueryProcedure(String mqttMessage, int camNumber);
    void publushResultProcedure(int camNumber, String eventType, String grz);
}
