package com.iqscaffold.billingservice.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import com.iqscaffold.billingservice.config.IqScaffoldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private MessageSource messageSource;

  @Mock
  private IqScaffoldProperties properties;

  @Mock
  private IqScaffoldProperties.I18n i18nProperties;

  private MessageService messageService;

  @BeforeEach
  void setUp() {
    lenient().when(properties.i18n()).thenReturn(i18nProperties);
    lenient().when(i18nProperties.getDefaultLocaleObject()).thenReturn(Locale.ENGLISH);
    lenient().when(i18nProperties.isLocaleSupported(any(Locale.class))).thenReturn(true);
    lenient().when(i18nProperties.isLocaleSupported(any(String.class))).thenReturn(true);

    messageService = new MessageService(messageSource, properties);
  }

  @Test
  void getMessage_shouldReturnMessageForCode() {
    // Given
    String code = "test.message";
    String expectedMessage = "Test Message";
    when(messageSource.getMessage(eq(code), eq(null), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(null), any(Locale.class));
  }

  @Test
  void getMessage_shouldReturnMessageWithArgs() {
    // Given
    String code = "test.message.args";
    Object[] args = {"arg1", "arg2"};
    String expectedMessage = "Test Message with arg1 and arg2";
    when(messageSource.getMessage(eq(code), eq(args), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, args);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(args), any(Locale.class));
  }

  @Test
  void getMessage_shouldReturnMessageForSpecificLocale() {
    // Given
    String code = "test.message";
    Locale locale = Locale.FRENCH;
    String expectedMessage = "Message de test";
    when(messageSource.getMessage(code, null, locale))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, locale);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(code, null, locale);
  }

  @Test
  void getMessage_shouldReturnMessageWithArgsAndLocale() {
    // Given
    String code = "test.message.args";
    Object[] args = {"arg1"};
    Locale locale = Locale.GERMAN;
    String expectedMessage = "Testnachricht mit arg1";
    when(messageSource.getMessage(code, args, locale))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, args, locale);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(code, args, locale);
  }

  @Test
  void getMessage_shouldReturnMessageForLocaleString() {
    // Given
    String code = "test.message";
    String localeString = "fr-FR";
    String expectedMessage = "Message français";
    when(messageSource.getMessage(eq(code), eq(null), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, localeString);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(null), any(Locale.class));
  }

  @Test
  void getMessage_shouldReturnMessageWithArgsAndLocaleString() {
    // Given
    String code = "test.message.args";
    Object[] args = {"value"};
    String localeString = "es-ES";
    String expectedMessage = "Mensaje con value";
    when(messageSource.getMessage(eq(code), eq(args), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, args, localeString);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(args), any(Locale.class));
  }

  @Test
  void getMessage_shouldFallbackToDefaultLocaleForUnsupportedLocale() {
    // Given
    String code = "test.message";
    String localeString = "unsupported-locale";
    String expectedMessage = "Default Message";

    when(i18nProperties.isLocaleSupported(any(Locale.class))).thenReturn(false);
    when(messageSource.getMessage(eq(code), eq(null), eq(Locale.ENGLISH)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, localeString);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(null), eq(Locale.ENGLISH));
  }

  @Test
  void getMessage_shouldHandleNullLocaleString() {
    // Given
    String code = "test.message";
    String expectedMessage = "Default Message";
    when(messageSource.getMessage(eq(code), eq(null), eq(Locale.ENGLISH)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, (String) null);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(null), eq(Locale.ENGLISH));
  }

  @Test
  void getMessage_shouldHandleBlankLocaleString() {
    // Given
    String code = "test.message";
    String expectedMessage = "Default Message";
    when(messageSource.getMessage(eq(code), eq(null), eq(Locale.ENGLISH)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, "   ");

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(null), eq(Locale.ENGLISH));
  }

  @Test
  void getMessage_shouldHandleEmptyArgs() {
    // Given
    String code = "test.message";
    Object[] args = {};
    String expectedMessage = "Test Message";
    when(messageSource.getMessage(eq(code), eq(args), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    String result = messageService.getMessage(code, args);

    // Then
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(code), eq(args), any(Locale.class));
  }
}
