package ru.artsec.ValidationGrzModuleV3.validate;

import org.springframework.validation.annotation.Validated;

@Validated
public interface Validates {

    String validateGRZ(String grz);
}
