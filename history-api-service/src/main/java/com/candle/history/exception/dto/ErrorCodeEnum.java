package com.candle.history.exception.dto;

import com.candle.history.exception.ErrorPrinter;
import org.springframework.http.HttpStatus;

public enum ErrorCodeEnum implements ErrorPrinter {

    DB_QUERY(HttpStatus.BAD_REQUEST),
    INVALID_PARAM(HttpStatus.BAD_REQUEST);

    private final HttpStatus httpStatus;

    ErrorCodeEnum(final HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
