package org.gripday.authservice.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.gripday.authservice.infrastructure.i18n.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Unit tests for MessageService. Tests message retrieval with different locales and parameters.
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

  @Mock
  private MessageSource messageSource;

  private MessageService messageService;

  @BeforeEach
  void setUp() {
    messageService = new MessageService(messageSource);
    LocaleContextHolder.setLocale(Locale.ENGLISH);
  }

  @Test
  void getMessage_WithCodeOnly_ShouldRetrieveMessage() {
    // Given
    var messageCode = "auth.login.success";
    var expectedMessage = "Login successful";
    when(messageSource.getMessage(eq(messageCode), eq(null), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode);

    // Then
    assertNotNull(result);
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(messageCode), eq(null), any(Locale.class));
  }

  @Test
  void getMessage_WithCodeAndArgs_ShouldRetrieveMessageWithParameters() {
    // Given
    var messageCode = "validation.min.length";
    var args = new Object[]{"Password", 8};
    var expectedMessage = "Password must be at least 8 characters";
    when(messageSource.getMessage(eq(messageCode), eq(args), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, args);

    // Then
    assertNotNull(result);
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(messageCode), eq(args), any(Locale.class));
  }

  @Test
  void getMessage_WithSpecificLocale_ShouldRetrieveLocalizedMessage() {
    // Given
    var messageCode = "auth.login.success";
    var locale = Locale.FRENCH;
    var expectedMessage = "Connexion réussie";
    when(messageSource.getMessage(eq(messageCode), eq(null), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, locale);

    // Then
    assertNotNull(result);
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(messageCode), eq(null), eq(locale));
  }

  @Test
  void getMessage_WithArgsAndLocale_ShouldRetrieveLocalizedMessageWithParameters() {
    // Given
    var messageCode = "email.verification.greeting";
    var args = new Object[]{"John"};
    var locale = Locale.forLanguageTag("es");
    var expectedMessage = "Hola John";
    when(messageSource.getMessage(eq(messageCode), eq(args), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, args, locale);

    // Then
    assertNotNull(result);
    assertEquals(expectedMessage, result);
    verify(messageSource).getMessage(eq(messageCode), eq(args), eq(locale));
  }

  @Test
  void getMessage_WithEnglishLocale_ShouldRetrieveEnglishMessage() {
    // Given
    var messageCode = "user.created";
    var locale = Locale.ENGLISH;
    var expectedMessage = "User created successfully";
    when(messageSource.getMessage(eq(messageCode), eq(null), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, locale);

    // Then
    assertEquals(expectedMessage, result);
  }

  @Test
  void getMessage_WithSpanishLocale_ShouldRetrieveSpanishMessage() {
    // Given
    var messageCode = "user.created";
    var locale = Locale.forLanguageTag("es");
    var expectedMessage = "Usuario creado exitosamente";
    when(messageSource.getMessage(eq(messageCode), eq(null), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, locale);

    // Then
    assertEquals(expectedMessage, result);
  }

  @Test
  void getMessage_WithFrenchLocale_ShouldRetrieveFrenchMessage() {
    // Given
    var messageCode = "user.created";
    var locale = Locale.FRENCH;
    var expectedMessage = "Utilisateur créé avec succès";
    when(messageSource.getMessage(eq(messageCode), eq(null), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, locale);

    // Then
    assertEquals(expectedMessage, result);
  }

  @Test
  void getMessage_WithMultipleParameters_ShouldFormatCorrectly() {
    // Given
    var messageCode = "validation.range";
    var args = new Object[]{"Age", 18, 65};
    var expectedMessage = "Age must be between 18 and 65";
    when(messageSource.getMessage(eq(messageCode), eq(args), any(Locale.class)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, args);

    // Then
    assertEquals(expectedMessage, result);
  }

  @Test
  void getMessage_WithEmailSubject_ShouldRetrieveCorrectMessage() {
    // Given
    var messageCode = "email.verification.subject";
    var locale = Locale.ENGLISH;
    var expectedMessage = "Verify your Gripday account";
    when(messageSource.getMessage(eq(messageCode), eq(null), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, locale);

    // Then
    assertEquals(expectedMessage, result);
  }

  @Test
  void getMessage_WithPasswordResetSubject_ShouldRetrieveCorrectMessage() {
    // Given
    var messageCode = "email.password.reset.subject";
    var locale = Locale.forLanguageTag("es");
    var expectedMessage = "Restablece tu contraseña de Gripday";
    when(messageSource.getMessage(eq(messageCode), eq(null), eq(locale)))
        .thenReturn(expectedMessage);

    // When
    var result = messageService.getMessage(messageCode, locale);

    // Then
    assertEquals(expectedMessage, result);
  }
}
