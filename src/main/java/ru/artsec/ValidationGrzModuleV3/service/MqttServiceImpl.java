package ru.artsec.ValidationGrzModuleV3.service;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class MqttServiceImpl  {

    private final static Logger log = LoggerFactory.getLogger(MqttServiceImpl.class);
    final MqttService mqttService;

    public MqttServiceImpl(MqttService mqttService) {
        this.mqttService = mqttService;
    }

    public void getConnectionMqttClient() throws MqttException {
        mqttService.getConnection();
        mqttService.getSubscribe();
    }
}
