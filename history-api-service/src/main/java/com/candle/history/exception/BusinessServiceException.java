package com.candle.history.exception;


import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public final class BusinessServiceException extends RuntimeException {

    final String message;

    final HttpStatus httpStatus;

    final transient ErrorPrinter errorEnum;

    public BusinessServiceException(final ErrorPrinter errorEnum, final String message) {
        super(message);
        this.message = message;
        this.errorEnum = errorEnum;
        this.httpStatus = errorEnum.getHttpStatus();
    }
}
