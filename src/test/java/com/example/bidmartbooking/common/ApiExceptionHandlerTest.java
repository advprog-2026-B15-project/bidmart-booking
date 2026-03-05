package com.example.bidmartbooking.common;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
    }

    @Test
    void shouldHandleResponseStatusExceptionWithReason() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found");

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatusException(exception);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("NOT_FOUND", response.getBody().get("code"));
        assertEquals("Booking not found", response.getBody().get("message"));
    }

    @Test
    void shouldHandleResponseStatusExceptionWithoutReason() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST);

        ResponseEntity<Map<String, Object>> response = handler.handleResponseStatusException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("BAD_REQUEST", response.getBody().get("code"));
        assertEquals("Bad Request", response.getBody().get("message"));
    }

    @Test
    void shouldHandleValidationExceptionWithFieldMessage() throws Exception {
        Method method = DummyValidationTarget.class.getDeclaredMethod("setRead", Boolean.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "read", "read is required"));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter,
                bindingResult
        );

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("BAD_REQUEST", response.getBody().get("code"));
        assertEquals("read is required", response.getBody().get("message"));
    }

    @Test
    void shouldHandleValidationExceptionWithoutFieldError() throws Exception {
        Method method = DummyValidationTarget.class.getDeclaredMethod("setRead", Boolean.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter,
                bindingResult
        );

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Validation failed", response.getBody().get("message"));
    }

    @Test
    void shouldHandleValidationExceptionWithNullDefaultMessage() throws Exception {
        Method method = DummyValidationTarget.class.getDeclaredMethod("setRead", Boolean.class);
        MethodParameter methodParameter = new MethodParameter(method, 0);

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "read", null, false, null, null, null));

        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                methodParameter,
                bindingResult
        );

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Validation failed", response.getBody().get("message"));
    }

    @Test
    void shouldHandleGenericException() {
        ResponseEntity<Map<String, Object>> response = handler.handleGenericException(
                new RuntimeException("boom")
        );

        assertEquals(500, response.getStatusCode().value());
        assertEquals("INTERNAL_ERROR", response.getBody().get("code"));
        assertEquals("Unexpected internal server error", response.getBody().get("message"));
    }

    static class DummyValidationTarget {
        public void setRead(Boolean read) {
            // only to provide method parameter metadata for test
        }
    }
}
