package com.stackcraft.retroflow.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .sorted().distinct().collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> invalidPathParameter(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getName() + " must be a valid UUID");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadableRequest(HttpMessageNotReadableException exception) {
        if (exception.getCause() instanceof JsonMappingException mappingException) {
            String path = mappingException.getPath().stream()
                .map(reference -> reference.getFieldName() == null ? "[" + reference.getIndex() + "]"
                    : reference.getFieldName())
                .collect(Collectors.joining("."));
            return error(HttpStatus.BAD_REQUEST, "Invalid value for " + (path.isEmpty() ? "request body" : path)
                + "; use UUIDs for IDs and OPEN or CLOSED for status");
        }
        return error(HttpStatus.BAD_REQUEST, "Request body is required and must contain valid JSON");
    }

    @ExceptionHandler(RetroflowException.class)
    public ResponseEntity<ErrorResponse> businessRuleViolation(RetroflowException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> resourceNotFound(ResourceNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<ErrorResponse> notImplemented(UnsupportedOperationException exception) {
        return error(HttpStatus.NOT_IMPLEMENTED, exception.getMessage());
    }

    private ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }

    public record ErrorResponse(String message) {
    }
}