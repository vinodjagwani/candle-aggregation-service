package com.candle.history.exception;

import com.candle.history.exception.dto.ErrorCodeEnum;
import com.candle.history.exception.dto.ErrorResponse;
import jakarta.validation.ValidationException;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import tools.jackson.databind.exc.InvalidFormatException;

@Slf4j
@RestControllerAdvice
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class DefaultExceptionHandler {

    private static final int SERVICE_CODE = 1000;
    private static final String EXCEPTION_OCCURRED_MSG = "An exception occurred: ";

    @ExceptionHandler({Throwable.class})
    public ResponseEntity<Object> handleException(final Throwable ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        return new ResponseEntity<>(getErrorResponse(ex.getMessage(), Collections.emptyList()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({WebExchangeBindException.class})
    public ResponseEntity<Object> handleException(final WebExchangeBindException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        List<ErrorResponse.ErrorInfo> errorInfos = new ArrayList<>();
        for (FieldError error : ex.getFieldErrors()) {
            errorInfos.add(ErrorResponse.ErrorInfo.builder().domain(error.getField())
                    .message(error.getDefaultMessage()).reason(ErrorCodeEnum.INVALID_PARAM.name()).build());
        }
        return new ResponseEntity<>(getErrorResponse(ErrorCodeEnum.INVALID_PARAM.name(), errorInfos),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({BusinessServiceException.class})
    public ResponseEntity<Object> handleException(final BusinessServiceException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        List<ErrorResponse.ErrorInfo> errorInfos = new ArrayList<>();
        errorInfos.add(ErrorResponse.ErrorInfo.builder().reason(ex.getErrorEnum().toString()).message(ex.getMessage())
                .domain("").build());
        return new ResponseEntity<>(getErrorResponse(ex.getErrorEnum().toString(), errorInfos), ex.getHttpStatus());
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<Object> handleException(final IllegalArgumentException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        return new ResponseEntity<>(getErrorResponse(ex.getMessage(), Collections.emptyList()), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler({ValidationException.class})
    public ResponseEntity<Object> handleException(final ValidationException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        final List<ErrorResponse.ErrorInfo> errorInfos = new ArrayList<>();
        final Throwable throwable = NestedExceptionUtils.getRootCause(ex);
        if (throwable instanceof BusinessServiceException bse) {
            return new ResponseEntity<>(
                    getErrorResponse(bse.getErrorEnum().toString(), errorInfos),
                    bse.getHttpStatus());
        }
        return new ResponseEntity<>(
                getErrorResponse(ex.getMessage(), errorInfos),
                HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleException(final MethodArgumentNotValidException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        final List<ErrorResponse.ErrorInfo> errorsInfo = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(
                objectError -> errorsInfo.add(ErrorResponse.ErrorInfo.builder().domain(objectError.getObjectName())
                        .message(objectError.getDefaultMessage()).reason(ErrorCodeEnum.INVALID_PARAM.name()).build()));
        return new ResponseEntity<>(getErrorResponse(ex.getMessage(), errorsInfo), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ResponseEntity<Object> handleException(final HttpMessageNotReadableException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        final Throwable throwable = NestedExceptionUtils.getRootCause(ex);
        if (throwable instanceof DateTimeParseException) {
            return new ResponseEntity<>(getErrorResponse(ex.getMessage(), Collections.emptyList()),
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }
        if (throwable instanceof InvalidFormatException) {
            return new ResponseEntity<>(getErrorResponse(ex.getMessage(), Collections.emptyList()),
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }
        return new ResponseEntity<>(getErrorResponse(ex.getMessage(), Collections.emptyList()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler({InvalidFormatException.class})
    public ResponseEntity<Object> handleException(final InvalidFormatException ex) {
        log.error(EXCEPTION_OCCURRED_MSG, ex);
        return new ResponseEntity<>(getErrorResponse(ex.getMessage(), Collections.emptyList()), HttpStatus.BAD_REQUEST);
    }

    private ErrorResponse getErrorResponse(final String message, final List<ErrorResponse.ErrorInfo> errorInfos) {
        return ErrorResponse.builder().message(message).code(SERVICE_CODE).errors(errorInfos).build();
    }
}
