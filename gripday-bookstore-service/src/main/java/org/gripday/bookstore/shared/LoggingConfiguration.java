package org.gripday.bookstore.shared;

import jakarta.annotation.PostConstruct;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.ConsoleAppender;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class LoggingConfiguration {

  private final Environment environment;

  @Value("${spring.profiles.active:local}")
  private String activeProfile;

  public LoggingConfiguration(final Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  public void configureLogging() {
    var loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    if (isProductionOrStaging()) {
      configureJsonLogging(loggerContext);
    } else {
      configureConsoleLogging(loggerContext);
    }
  }

  private boolean isProductionOrStaging() {
    return "production".equals(activeProfile) || "staging".equals(activeProfile);
  }

  private void configureJsonLogging(LoggerContext loggerContext) {
    var rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    // Remove existing appenders
    rootLogger.detachAndStopAllAppenders();

    // Create JSON console appender
    var jsonAppender = new ConsoleAppender<ILoggingEvent>();
    jsonAppender.setContext(loggerContext);
    jsonAppender.setName("JSON_CONSOLE");

    var jsonLayout = new JsonLayout();
    jsonLayout.setContext(loggerContext);
    jsonLayout.setIncludeMDC(true);
    jsonLayout.setIncludeTimestamp(true);
    jsonLayout.setTimestampFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    jsonLayout.setIncludeLevel(true);
    // Thread information will be included in the JSON output automatically
    jsonLayout.setIncludeLoggerName(true);
    jsonLayout.setIncludeFormattedMessage(true);
    jsonLayout.setIncludeException(true);
    jsonLayout.start();

    jsonAppender.setLayout(jsonLayout);
    jsonAppender.start();

    rootLogger.addAppender(jsonAppender);
  }

  private void configureConsoleLogging(LoggerContext loggerContext) {
    var rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

    // Keep existing console appender but ensure proper pattern
    var consoleAppender = new ConsoleAppender<ILoggingEvent>();
    consoleAppender.setContext(loggerContext);
    consoleAppender.setName("CONSOLE");

    var encoder = new PatternLayoutEncoder();
    encoder.setContext(loggerContext);
    encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{" + BookstoreConstants.MdcKeys.CORRELATION_ID + "}] [%X{" + BookstoreConstants.MdcKeys.USER_ID + "}:%X{" + BookstoreConstants.MdcKeys.USERNAME + "}] %logger{36} - %msg%n");
    encoder.start();

    consoleAppender.setEncoder(encoder);
    consoleAppender.start();

    // Only add if not already present
    if (rootLogger.getAppender("CONSOLE") == null) {
      rootLogger.addAppender(consoleAppender);
    }
  }
}