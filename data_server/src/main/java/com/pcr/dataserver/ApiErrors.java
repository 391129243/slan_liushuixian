package com.pcr.dataserver;

import java.util.Map;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log=LoggerFactory.getLogger(ApiErrors.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> api(ApiException error) { return response(error.status(),error.getMessage()); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> size(Exception error) { return response(HttpStatus.PAYLOAD_TOO_LARGE,"Upload too large"); }
    @ExceptionHandler({MethodArgumentNotValidException.class,MissingServletRequestPartException.class,
        HandlerMethodValidationException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.http.converter.HttpMessageNotReadableException.class})
    ResponseEntity<?> invalid(Exception error) { return response(HttpStatus.BAD_REQUEST,"Invalid request fields"); }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    ResponseEntity<?> media(Exception error) { return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Use application/json or multipart/form-data"); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> method(Exception error) { return response(HttpStatus.METHOD_NOT_ALLOWED,"Unsupported HTTP method"); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception error) {
        log.error("Request failed: {}",error.getClass().getSimpleName());
        return response(HttpStatus.INTERNAL_SERVER_ERROR,"Server could not persist the upload");
    }
    private ResponseEntity<?> response(HttpStatus status,String message) {
        return ResponseEntity.status(status).body(Map.of("Result","0","Message",message));
    }
}
