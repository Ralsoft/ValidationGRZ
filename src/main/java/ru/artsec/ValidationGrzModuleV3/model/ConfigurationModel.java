package ru.artsec.ValidationGrzModuleV3.model;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ConfigurationModel {
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

    public ConfigurationModel() {
    }
}
