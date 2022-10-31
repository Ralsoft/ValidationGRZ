package ru.artsec.ValidationGrzModuleV3.model;

import lombok.Data;

@Data
public class Message {
    public byte x;
    public byte y;
    public byte color;
    public String text;

    public Message(){

    }

    public Message(byte x, byte y, byte color, String text) {
        this.x = x;
        this.y = y;
        this.color = color;
        this.text = text;
    }
}
