package com.iqscaffold.userservice.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;

import jakarta.validation.constraints.NotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;

import com.iqscaffold.userservice.usermanagement.User;


class MessageServiceTest {

  static class StubMessageSource implements MessageSource {
    @Override
    public String getMessage(@NotNull String code, Object[] args, String defaultMessage, Locale locale) {
      return format(code, args, locale);
    }

    @Override
    public @NotNull String getMessage(@NotNull String code, Object[] args, Locale locale) {
      return format(code, args, locale);
    }

    @Override
    public @NotNull String getMessage(MessageSourceResolvable resolvable, Locale locale) {
      String code;
      Object[] args = null;
      if (resolvable != null) {
        String[] codes = resolvable.getCodes();
        code = (codes != null && codes.length > 0) ? codes[0] : "unknown";
        args = resolvable.getArguments();
      } else {
        code = "unknown";
      }
      return format(code, args, locale);
    }

    private String format(String code, Object[] args, Locale locale) {
      var argsStr = (args == null || args.length == 0) ? "" : ("[" + java.util.Arrays.stream(args).map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("") + "]");
      var loc = (locale == null) ? "null" : locale.toLanguageTag();
      return code + argsStr + "@" + loc;
    }
  }

  @AfterEach
  void cleanupLocale() {
    LocaleContextHolder.resetLocaleContext();
  }

  @Test
  void getMessageShouldUseLocaleContextHolder() {
    var ms = new MessageService(new StubMessageSource());

    LocaleContextHolder.setLocale(Locale.FRANCE);
    assertEquals("greeting@fr-FR", ms.getMessage("greeting"));

    LocaleContextHolder.setLocale(Locale.US);
    assertEquals("greeting@en-US", ms.getMessage("greeting"));
  }

  @Test
  void getMessageWithArgsShouldRenderArgs() {
    var ms = new MessageService(new StubMessageSource());

    LocaleContextHolder.setLocale(Locale.GERMANY);
    assertEquals("hello[John,3]@de-DE", ms.getMessage("hello", new Object[] {"John", 3}));
  }

  @Test
  void getMessageWithExplicitLocaleShouldBypassContext() {
    var ms = new MessageService(new StubMessageSource());

    LocaleContextHolder.setLocale(Locale.JAPAN);
    assertEquals("key@en-GB", ms.getMessage("key", Locale.UK));
    assertEquals("key[1]@en-GB", ms.getMessage("key", new Object[] {1}, Locale.UK));
  }

  @Test
  void getMessageWithUserShouldUseUserPreferredLocale() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    when(user.getPreferredLocale()).thenReturn("es-ES");

    LocaleContextHolder.setLocale(Locale.JAPAN);
    assertEquals("greeting@es-ES", ms.getMessage("greeting", user));
  }

  @Test
  void getMessageWithUserAndArgsShouldUseUserPreferredLocale() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    when(user.getPreferredLocale()).thenReturn("fr-FR");

    LocaleContextHolder.setLocale(Locale.JAPAN);
    assertEquals("hello[Alice,5]@fr-FR", ms.getMessage("hello", new Object[] {"Alice", 5}, user));
  }

  @Test
  void getUserLocaleShouldReturnUserPreferredLocale() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    when(user.getPreferredLocale()).thenReturn("de-DE");

    assertEquals(Locale.forLanguageTag("de-DE"), ms.getUserLocale(user));
  }

  @Test
  void getUserLocaleShouldFallbackToContextWhenUserIsNull() {
    var ms = new MessageService(new StubMessageSource());
    LocaleContextHolder.setLocale(Locale.ITALY);

    assertEquals(Locale.ITALY, ms.getUserLocale(null));
  }

  @Test
  void getUserLocaleShouldFallbackToContextWhenUserLocaleIsEmpty() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    when(user.getPreferredLocale()).thenReturn("");
    LocaleContextHolder.setLocale(Locale.CANADA);

    assertEquals(Locale.CANADA, ms.getUserLocale(user));
  }

  @Test
  void getUserLocaleShouldFallbackToContextWhenUserLocaleIsNull() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    when(user.getPreferredLocale()).thenReturn(null);
    LocaleContextHolder.setLocale(Locale.KOREA);

    assertEquals(Locale.KOREA, ms.getUserLocale(user));
  }

  @Test
  void getUserLocaleShouldFallbackToDefaultWhenContextLocaleIsNull() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    when(user.getPreferredLocale()).thenReturn(null);
    LocaleContextHolder.resetLocaleContext();

    var result = ms.getUserLocale(user);
    // When context is null, it returns the default locale (typically en_US or en)
    assertEquals(Locale.getDefault(), result);
  }

  @Test
  void getUserLocaleShouldHandleInvalidUserLocale() {
    var ms = new MessageService(new StubMessageSource());
    var user = mock(User.class);
    // Locale.forLanguageTag doesn't throw exception, it creates a locale with the tag
    when(user.getPreferredLocale()).thenReturn("invalid-locale-tag");

    var result = ms.getUserLocale(user);
    // The result should be a locale created from the invalid tag
    assertEquals(Locale.forLanguageTag("invalid-locale-tag"), result);
  }
}
