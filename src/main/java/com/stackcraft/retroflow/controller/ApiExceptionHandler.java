package com.stackcraft.retroflow.controller;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.stackcraft.retroflow.dto.ErrorResponse;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
            .map(error -> error instanceof FieldError fieldError
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : error.getDefaultMessage())
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
                + expectedValues(mappingException));
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> unknownRoute(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "No endpoint " + request.getMethod() + " " + request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .allow(exception.getSupportedHttpMethods() == null ? new HttpMethod[0]
                : exception.getSupportedHttpMethods().toArray(HttpMethod[]::new))
            .body(new ErrorResponse("Method " + exception.getMethod() + " is not supported for this endpoint"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type must be application/json");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception exception) {
        // Framework client errors without a dedicated handler keep their own status instead of becoming a 500.
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError
                && frameworkError.getStatusCode().is4xxClientError()) {
            String detail = frameworkError.getBody().getDetail();
            HttpStatus status = HttpStatus.resolve(frameworkError.getStatusCode().value());
            return error(frameworkError.getStatusCode(), detail != null ? detail
                : status != null ? status.getReasonPhrase() : "Request failed");
        }
        LOGGER.error("Unexpected error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
    }

    private static String expectedValues(JsonMappingException exception) {
        Class<?> target = exception instanceof InvalidFormatException invalidFormat ? invalidFormat.getTargetType() : null;
        if (target == null) {
            return "";
        }
        if (target.isEnum()) {
            return "; use one of " + Arrays.stream(target.getEnumConstants()).map(Object::toString)
                .collect(Collectors.joining(", "));
        }
        if (target == UUID.class) {
            return "; use a valid UUID";
        }
        if (target == Boolean.class || target == boolean.class) {
            return "; use true or false";
        }
        return "";
    }

    private ResponseEntity<ErrorResponse> error(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}