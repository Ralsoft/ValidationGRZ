package ru.artsec.ValidationGrzModuleV3.validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ValidatesImpl implements Validates{

    private final static Logger log = LoggerFactory.getLogger(Validates.class);
    @Override
    public String validateGRZ(String grz) {
        log.info("Валидация прошла успешно.");
        return grz;
    }
}
