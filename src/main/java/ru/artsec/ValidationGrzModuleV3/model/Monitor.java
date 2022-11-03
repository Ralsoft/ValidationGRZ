package ru.artsec.ValidationGrzModuleV3.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
public class Monitor {
    public int camNumber;
    public List<Message> messages;

    public Monitor() {

    }

    public Monitor(int camNumber, List<Message> messages) {
        this.camNumber = camNumber;
        this.messages = messages;
    }
}
