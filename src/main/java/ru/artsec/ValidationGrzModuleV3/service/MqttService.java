package ru.artsec.ValidationGrzModuleV3.service;

import org.eclipse.paho.client.mqttv3.MqttException;

import java.sql.SQLException;

public interface MqttService {
    void getConnection(String name) throws InterruptedException;

    void getSubscribe() throws MqttException;

    void implementQueryProcedure(String mqttMessage, int camNumber) throws InterruptedException, SQLException;

    void publishResultProcedure(int camNumber, String eventType, String grz);
}
