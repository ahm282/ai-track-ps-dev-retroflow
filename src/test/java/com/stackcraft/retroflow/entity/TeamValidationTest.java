package com.stackcraft.retroflow.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TeamValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void namesWithSurroundingSpaces_areValid() {
        // Arrange
        Team team = new Team(null, " Team Alpha ", null, List.of(new Member(null, " Alice ", true, null)), null);

        // Act
        Set<ConstraintViolation<Team>> violations = validator.validate(team);

        // Assert
        assertThat(violations).isEmpty();
    }
}
