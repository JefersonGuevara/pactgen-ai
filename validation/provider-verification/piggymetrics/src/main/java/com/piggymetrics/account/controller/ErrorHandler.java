// Portado de piggymetrics (6bb2cf9) account-service/src/main/java/com/piggymetrics/account/controller/ErrorHandler.java. Adaptaciones: ver NOTICE.
package com.piggymetrics.account.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class ErrorHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

	// TODO add MethodArgumentNotValidException handler
	// TODO remove such general handler
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public void processValidationError(IllegalArgumentException e) {
		log.info("Returning HTTP 400 Bad Request", e);
	}
}
