package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

final class EntityValidation {

    private EntityValidation() {
    }

    static void require(Validator validator, Object entity) {
        Set<ConstraintViolation<Object>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            throw new RetroflowException(violations.stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted().distinct().collect(Collectors.joining("; ")));
        }
    }
}
