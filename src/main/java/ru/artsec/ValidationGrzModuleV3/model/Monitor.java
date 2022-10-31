package ru.artsec.ValidationGrzModuleV3.model;

import lombok.*;

import java.util.List;

@Data
public class Monitor {
    public String camNumber;
    public String ip;
    public int port;
    public Message messages;

    public Monitor(){

    }

    public Monitor(String camNumber, String ip, int port, Message messages) {
        this.camNumber = camNumber;
        this.ip = ip;
        this.port = port;
        this.messages = messages;
    }
}
