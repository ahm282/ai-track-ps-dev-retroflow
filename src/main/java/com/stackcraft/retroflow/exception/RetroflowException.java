package com.stackcraft.retroflow.exception;

/** Thrown when a request violates a business rule; the handler maps it to 400. */
public class RetroflowException extends RuntimeException {

    public RetroflowException(String message) {
        super(message);
    }

}
