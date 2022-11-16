package ru.artsec.ValidationGrzModuleV3.model;

import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ConfigurationModel {
    String mqttUsername = "admin";
    String mqttPassword = "333";
    String mqttClientId = "Validation";
    String mqttClientIp = "194.87.237.67";
    int mqttClientPort = 1883;
    String databaseLogin = "SYSDBA";
    String databasePassword = "temp";
    //    String databasePath = "C:\\\\Program Files (x86)\\\\CardSoft\\\\DuoSE\\\\Access\\\\ShieldPro_rest.gdb";
    String databasePath = "C:\\\\ttt\\\\111.GDB";
    //    String databaseIp = "127.0.0.1";
    String databaseIp = "zet-buharov";
    int databasePort = 3050;

    Map<Integer, Integer> cameraIdDeviceIdDictionary = new HashMap<>() {{
        put(1, 365);
    }};


    Map<String, List<Message>> stringDictionary = new HashMap<>() {{
        put("65", List.of(new Message((byte) 0x09, (byte) 0x00, (byte) 0x02, "Недействительная карточка")));
        put("50", List.of(new Message((byte) 0x09, (byte) 0x00, (byte) 0x02, "Действительная карточка")));
        put("46", List.of(new Message((byte) 0x09, (byte) 0x00, (byte) 0x02, "Неизвестная карточка")));
    }};


    public ConfigurationModel() {
    }
}
