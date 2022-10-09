package com.wejuai.core.config;

import com.endofmaster.commons.util.validate.ValidateParamIsNullException;
import com.endofmaster.commons.util.validate.ValidateStringIsBlankException;
import com.endofmaster.rest.exceptionhandler.NoErrorCodeRestExceptionHandler;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.wejuai.exception.IllegalParameterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

import java.util.NoSuchElementException;

/**
 * @author YQ.Huang
 */
@RestControllerAdvice
class ErrorControllerAdvice extends NoErrorCodeRestExceptionHandler {

    private final static Logger logger = LoggerFactory.getLogger(ErrorControllerAdvice.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Object> handleException(ResourceAccessException e) {
        logger.error("Handle ResourceAccessException", e);
        return new ResponseEntity<>("服务暂不可用，请稍后再试", new HttpHeaders(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(ValidateStringIsBlankException.class)
    public ResponseEntity<Object> handleException(ValidateStringIsBlankException e) {
        logger.error("Handle ValidateStringIsBlankException", e);
        return new ResponseEntity<>(e.getLocalizedMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidateParamIsNullException.class)
    public ResponseEntity<Object> handleException(ValidateParamIsNullException e) {
        logger.error("Handle ValidateParamIsNullException", e);
        return new ResponseEntity<>(e.getLocalizedMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalParameterException.class)
    public ResponseEntity<Object> handleException(IllegalParameterException e) {
        logger.error("Handle IllegalParameterException", e);
        return new ResponseEntity<>(e.getLocalizedMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ResponseEntity<Object> handleException(InvalidFormatException e) {
        logger.error("Handle InvalidFormatException", e);
        return new ResponseEntity<>(e.getLocalizedMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleException(NoSuchElementException e) {
        logger.error("Handle InvalidFormatException", e);
        return new ResponseEntity<>(e.getLocalizedMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

}
