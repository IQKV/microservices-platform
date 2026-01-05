# Email Templates - IQ Scaffold Billing Service

This directory contains professionally designed email templates for the IQ Scaffold Billing Service. All templates follow modern design principles and are consistent with the user service email templates.

## 📧 Available Templates

### 1. Merchant Onboarding (`merchant-onboarding.html`)
**Purpose**: Welcome new merchants and guide them through the Stripe account setup process.

**Variables**:
- `${merchantName}` - Name of the merchant/business
- `${merchantEmail}` - Merchant's email address
- `${onboardingUrl}` - Stripe onboarding completion URL

**Features**:
- Professional welcome design with rocket icon
- Clear next steps section
- Security notice about link expiration
- Alternative link fallback
- Responsive design

### 2. Payment Refunded (`payment-refunded.html`)
**Purpose**: Notify customers when a payment refund has been processed.

**Variables**:
- `${amount}` - Refund amount
- `${currency}` - Currency code (USD, EUR, etc.)
- `${paymentId}` - Original payment identifier
- `${refundId}` - Refund transaction identifier (optional)
- `${refundDate}` - Date when refund was processed (optional)

**Features**:
- Clear refund amount highlighting
- Detailed refund information table
- Processing timeline information
- Support contact information
- Professional money icon

### 3. Payment Successful (`payment-successful.html`)
**Purpose**: Confirm successful payment processing to customers.

**Variables**:
- `${amount}` - Payment amount
- `${currency}` - Currency code
- `${paymentId}` - Payment identifier
- `${description}` - Payment description (optional)
- `${paymentDate}` - Payment processing date (optional)
- `${paymentMethod}` - Payment method used (optional)
- `${receiptUrl}` - Download receipt URL (optional)

**Features**:
- Success confirmation with checkmark icon
- Payment details table
- Optional receipt download button
- Support information
- Clean, professional design

### 4. Payment Failed (`payment-failed.html`)
**Purpose**: Notify customers of failed payment attempts and provide troubleshooting guidance.

**Variables**:
- `${amount}` - Attempted payment amount
- `${currency}` - Currency code
- `${paymentId}` - Payment attempt identifier
- `${description}` - Payment description (optional)
- `${attemptDate}` - Date of payment attempt (optional)
- `${errorMessage}` - Specific error message (optional)
- `${retryUrl}` - URL to retry payment (optional)

**Features**:
- Clear error indication with X icon
- Troubleshooting steps
- Retry payment button (optional)
- Support contact information
- Error-focused color scheme

### 5. Invoice Generated (`invoice-generated.html`)
**Purpose**: Notify customers when a new invoice is available for payment.

**Variables**:
- `${invoiceNumber}` - Invoice identifier
- `${amount}` - Invoice amount
- `${currency}` - Currency code
- `${issueDate}` - Invoice issue date (optional)
- `${dueDate}` - Payment due date (optional)
- `${description}` - Invoice description (optional)
- `${invoiceUrl}` - Download invoice URL (optional)

**Features**:
- Invoice document icon
- Due date highlighting
- Payment methods information
- Download invoice button
- Professional billing design

## 🎨 Design Features

All templates share these design characteristics:

### Visual Design
- **Modern Typography**: System fonts (-apple-system, BlinkMacSystemFont, Segoe UI)
- **Consistent Branding**: IQ Scaffold logo and color scheme
- **Responsive Layout**: Mobile-friendly design with max-width: 600px
- **Professional Icons**: Contextual emoji icons for each email type
- **Color Coding**: Semantic colors (green for success, red for errors, blue for information)

### Layout Structure
- **Header Section**: Logo, icon, and title
- **Content Section**: Main message and details
- **Action Buttons**: Primary call-to-action buttons
- **Information Panels**: Highlighted sections for important details
- **Footer Section**: Contact information and copyright

### Accessibility
- **High Contrast**: Sufficient color contrast for readability
- **Clear Hierarchy**: Proper heading structure and visual hierarchy
- **Readable Fonts**: Large, clear typography
- **Alternative Text**: Descriptive content for screen readers

## 🌐 Internationalization

All text content is externalized to `src/main/resources/i18n/messages.properties`:

```properties
# Example message keys
email.payment.successful.subject=Payment Successful - IQ Scaffold
email.payment.successful.title=Payment Successful
email.payment.successful.greeting=Hello,
email.payment.successful.body=Thank you for your payment! We have successfully processed your transaction.
```

### Adding New Languages

1. Create new properties file: `messages_es.properties` (for Spanish)
2. Translate all message keys
3. Templates will automatically use the appropriate language based on user locale

## 🔧 Technical Implementation

### Thymeleaf Integration

Templates use Thymeleaf templating engine with these features:

```html
<!-- Internationalized text -->
<h1 th:text="#{email.payment.successful.title}">Payment Successful</h1>

<!-- Variable substitution -->
<span th:text="${paymentId}">123456789</span>

<!-- Conditional rendering -->
<div th:if="${receiptUrl}">
  <a th:href="${receiptUrl}">Download Receipt</a>
</div>

<!-- Date formatting -->
<span th:text="${#temporals.format(paymentDate, 'MMM dd, yyyy HH:mm')}">Jan 15, 2024 14:30</span>
```

### CSS Styling

- **Inline CSS**: All styles are inline for maximum email client compatibility
- **Email-Safe CSS**: Only email-client-supported CSS properties
- **Fallback Colors**: Hex colors for maximum compatibility
- **Table-Based Layout**: Fallback for older email clients

### Email Client Compatibility

Templates are tested and compatible with:
- ✅ Gmail (Web, Mobile, App)
- ✅ Outlook (2016+, Web, Mobile)
- ✅ Apple Mail (macOS, iOS)
- ✅ Yahoo Mail
- ✅ Thunderbird
- ✅ Mobile email clients

## 📝 Usage Examples

### Java Service Integration

```java
@Service
public class EmailService {
    
    @Autowired
    private TemplateEngine templateEngine;
    
    public void sendPaymentSuccessEmail(String email, PaymentDetails payment) {
        Context context = new Context();
        context.setVariable("amount", payment.getAmount());
        context.setVariable("currency", payment.getCurrency());
        context.setVariable("paymentId", payment.getId());
        context.setVariable("paymentDate", payment.getCreatedAt());
        
        String htmlContent = templateEngine.process("email/payment-successful", context);
        
        // Send email using your email service
        emailSender.send(email, "Payment Successful", htmlContent);
    }
}
```

### Template Context Variables

Each template expects specific context variables. Always provide:

**Required Variables**:
- Template-specific required fields (see individual template documentation)

**Optional Variables**:
- Additional context that enhances the email experience
- URLs for actions (download, retry, etc.)
- Dates and timestamps
- Descriptive text

## 🚀 Best Practices

### Content Guidelines
1. **Clear Subject Lines**: Use descriptive, action-oriented subjects
2. **Concise Messaging**: Keep content focused and scannable
3. **Strong CTAs**: Use clear, action-oriented button text
4. **Professional Tone**: Maintain consistent brand voice
5. **Error Handling**: Provide helpful troubleshooting information

### Technical Guidelines
1. **Variable Validation**: Always validate template variables before rendering
2. **Fallback Content**: Provide default values for optional variables
3. **Error Handling**: Gracefully handle missing or invalid data
4. **Testing**: Test templates with various data scenarios
5. **Performance**: Cache compiled templates for better performance

### Security Considerations
1. **Input Sanitization**: Sanitize all user-provided content
2. **URL Validation**: Validate all URLs before including in emails
3. **Sensitive Data**: Never include sensitive information in emails
4. **Token Expiration**: Use time-limited tokens for action URLs

## 🔄 Maintenance

### Regular Updates
- Review and update templates quarterly
- Test with new email clients as they're released
- Update branding elements as needed
- Refresh content based on user feedback

### Monitoring
- Track email open rates and click-through rates
- Monitor for delivery issues
- Collect user feedback on email clarity
- A/B test subject lines and content

---

**Note**: These templates are designed to work seamlessly with the IQ Scaffold platform's email infrastructure and follow the same design principles as the user service email templates.