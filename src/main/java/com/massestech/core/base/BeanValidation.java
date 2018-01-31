package com.massestech.core.base;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by ethan on 5/25/16.
 */
public class BeanValidation {

    public static List<String> validate(Object m) {

        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();

        Set<ConstraintViolation<Object>> constraints = validator.validate(m);

        List<String> errors = new ArrayList<>();

        if (!constraints.isEmpty()) {

            for (ConstraintViolation<Object> constraint : constraints) {

                if(constraint.getMessageTemplate().indexOf("org.hibernate.validator") < 0
                        && constraint.getMessageTemplate().indexOf("javax.validation") < 0){
                    errors.add(constraint.getMessageTemplate());
                }else {
                    errors.add(constraint.getPropertyPath() + constraint.getMessage());
                }
            }
        }
        return errors;
    }

    public static String toErrorsStr(List<String> errors){
        String errorsStr = "";
        for (String str : errors){
            errorsStr += str + ",";
        }
        return errorsStr.substring(0, errorsStr.length() - 1);
    }
}
