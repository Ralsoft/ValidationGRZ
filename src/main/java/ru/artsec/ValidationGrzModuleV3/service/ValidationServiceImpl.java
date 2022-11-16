package ru.artsec.ValidationGrzModuleV3.service;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidationServiceImpl {

    private final static Logger log = LoggerFactory.getLogger(ValidationServiceImpl.class);
    final MqttService mqttService;

    public ValidationServiceImpl(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    public void getConnectionMqttClient() throws InterruptedException {
        mqttService.getConnection();

    }


}
