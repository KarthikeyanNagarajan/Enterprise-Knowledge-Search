package com.karthik.eks.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a required resource is not found.
 */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
