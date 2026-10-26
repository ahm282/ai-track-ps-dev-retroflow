package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    @Test
    void anObjectLevelValidationError_isReportedInTheMessage() {
        // Arrange
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new ObjectError("request", "Priority is required for action items"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        // Act
        ResponseEntity<ErrorResponse> response = new ApiExceptionHandler().invalidRequest(exception);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(new ErrorResponse("Priority is required for action items"));
    }
}
