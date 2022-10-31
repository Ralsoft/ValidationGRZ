package ru.artsec.ValidationGrzModuleV3.validate;

import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.Pattern;

@Validated
public interface Validates {

    String validateGRZ(@Pattern(regexp = "^[ABCDEFHKMOPTXY\\d]+$", message = "Введен неправильный ГРЗ!")
                       String grz);
}
