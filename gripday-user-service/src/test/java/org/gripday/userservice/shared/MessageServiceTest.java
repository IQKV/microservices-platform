package org.gripday.userservice.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;

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
}
