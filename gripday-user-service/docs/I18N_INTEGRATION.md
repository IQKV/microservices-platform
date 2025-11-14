# Internationalization (i18n) Integration

## Overview

The Gripday User Service now supports internationalization for emails and API responses. Users receive localized emails based on their preferred locale stored in the database.

## Supported Locales

- **English (en)** - Default
- **Spanish (es)**
- **French (fr)**

## Architecture

### Components

1. **MessageService** - Service for retrieving localized messages
2. **I18nConfig** - Configuration for message sources and locale resolution
3. **User.preferredLocale** - Database field storing user's language preference
4. **Message Bundles** - Properties files in `src/main/resources/i18n/`

### Email Localization

The `EmailService` automatically uses the user's preferred locale when sending:

- Verification emails
- Password reset emails

The locale is determined by:

1. User's `preferredLocale` field in the database
2. Falls back to English if not set or invalid

### Message Keys

All message keys are defined in `messages.properties` files:

```properties
# Authentication
auth.login.success=Login successful
auth.login.failed=Invalid username or password

# Email
email.verification.subject=Verify your Gripday account
email.verification.greeting=Hello {0}
email.password.reset.subject=Reset your Gripday password
```

## Usage Examples

### In Services

```java
@Service
public class MyService {

  private final MessageService messageService;

  public MyService(MessageService messageService) {
    this.messageService = messageService;
  }

  public String getLocalizedMessage() {
    // Current locale from LocaleContextHolder
    return messageService.getMessage("user.created");
  }

  public String getMessageWithParams() {
    return messageService.getMessage("validation.min.length", new Object[] { "Password", 8 });
  }

  public String getMessageForLocale() {
    return messageService.getMessage("auth.login.success", Locale.FRENCH);
  }
}
```

### Setting User Locale

Users can set their preferred locale via the User entity:

```java
user.setPreferredLocale("es"); // Spanish
user.setPreferredLocale("fr"); // French
user.setPreferredLocale("en"); // English
```

### API Locale Resolution

The API automatically resolves locale from the `Accept-Language` HTTP header for REST endpoints.

## Database Migration

A Liquibase migration adds the `preferred_locale` column:

```xml
<!-- 010-add-user-preferred-locale.xml -->
<addColumn tableName="users">
  <column name="preferred_locale" type="VARCHAR(10)" defaultValue="en"/>
</addColumn>
```

## Adding New Languages

1. Create new message bundle: `messages_{locale}.properties`
2. Add locale to `I18nConfig.localeResolver()` supported locales
3. Translate all message keys

Example for German:

```java
// I18nConfig.java
localeResolver.setSupportedLocales(List.of(
  Locale.ENGLISH,
  Locale.forLanguageTag("es"),
  Locale.FRENCH,
  Locale.GERMAN  // Add new locale
));
```

## Email Templates

Email templates use Thymeleaf with localized variables:

```html
<p th:text="${greeting}">Hello User,</p>
<p th:text="${body}">Email body text</p>
<a th:href="${verificationUrl}" th:text="${buttonText}">Button</a>
```

All text content is injected from message bundles based on user's locale.
