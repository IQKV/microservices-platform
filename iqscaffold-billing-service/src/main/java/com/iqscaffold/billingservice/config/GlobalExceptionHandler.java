package com.iqscaffold.billingservice.config;

import java.net.URI;
import java.time.Instant;

import com.iqscaffold.billingservice.shared.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private final MessageService messageService;

  public GlobalExceptionHandler(MessageService messageService) {
    this.messageService = messageService;
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail handleUnhandled(Exception e) {
    ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        messageService.getMessage("error.unexpected")
    );
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setType(URI.create("urn:problem-type:internal-server-error"));
    problemDetail.setProperty("timestamp", Instant.now());
    return problemDetail;
  }
}
