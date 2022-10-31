package ru.artsec.ValidationGrzModuleV3.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
public class ValidationInfo {
    public String GRZ;

    public ValidationInfo(String GRZ, String camNumber) {
        this.GRZ = GRZ;
        this.camNumber = camNumber;
    }

    public String camNumber;

}
