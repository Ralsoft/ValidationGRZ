package ru.artsec.ValidationGrzModuleV3.model;

import lombok.Data;

@Data
public class MQTTClientModel {
    String clientId;
    String hostName;
    int port;

    public MQTTClientModel() {
    }

    @Override
    public String toString() {
        return "{" + "\n" +
                "\"clientId\": \"" + clientId + "\",\n" +
                "\"hostName\": \"" + hostName + "\",\n" +
                "\"port\": " + port + "\n" +
                '}';
    }

    public MQTTClientModel(String clientId, String hostName, int port) {
        this.clientId = clientId;
        this.hostName = hostName;
        this.port = port;
    }
}
