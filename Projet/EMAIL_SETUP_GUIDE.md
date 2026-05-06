# EMAIL NOTIFICATION SETUP GUIDE

## 🎯 Feature Overview

Email notifications are now implemented for the reservation system. When a user creates a reservation in the front office, an automatic email is sent to the administrator.

## 📧 Email Configuration

### 1. Gmail Setup (Required)

**IMPORTANT**: You must configure Gmail credentials in `EmailService.java` before using this feature.

#### Steps to configure Gmail:

1. **Enable 2-Factor Authentication** on your Gmail account
2. **Generate App Password**:
   - Go to Google Account settings
   - Security → 2-Step Verification → App passwords
   - Generate a password for "Mail"
   - Copy the 16-character password

3. **Update EmailService.java**:
   ```java
   private final String GMAIL_USERNAME = "ihebjbir57@gmail.com";
   private final String GMAIL_APP_PASSWORD = "your-16-char-app-password";
   ```

### 2. Admin Email

The notification emails are sent to: `farah.hannachi@esprit.tn`

To change this, modify the `ADMIN_EMAIL` constant in `EmailService.java`.

## 📨 Email Content

### Subject
```
Reservation Confirmation - CuraVita
```

### Body (HTML format)
- Reservation ID
- Client name
- Service name and type
- Appointment date & time
- Status
- Motif/Reason

## 🔄 Integration Points

### Automatic Triggers
- Email is sent **automatically** after successful reservation creation
- No user interaction required
- Email failure does NOT prevent reservation from being saved

### Error Handling
- If email fails, reservation is still saved
- Errors are logged to console
- UI remains responsive

## 🧪 Testing

### Test Email Sending
1. Create a reservation through the front office
2. Check console for success message: "Email de confirmation envoyé pour la réservation #X"
3. Verify email arrives at admin inbox

### Test Error Scenarios
1. Configure invalid Gmail credentials
2. Create reservation
3. Verify reservation is saved despite email failure
4. Check console for error messages

## 🔧 Troubleshooting

### Common Issues

**"Authentication failed"**
- Verify Gmail username and app password
- Ensure 2FA is enabled
- Regenerate app password if needed

**"Connection timeout"**
- Check internet connection
- Verify Gmail SMTP settings (should work as configured)
- **Network firewall may be blocking SMTP ports** (see NETWORK_CONNECTIVITY_FIX.md)

**"Service not found"**
- Ensure service exists in database
- Check service_id in reservation table

### Debug Mode
Add this to EmailService for debugging:
```java
session.setDebug(true);
```

## 📋 Dependencies Added

```xml
<dependency>
    <groupId>com.sun.mail</groupId>
    <artifactId>jakarta.mail</artifactId>
    <version>2.0.1</version>
</dependency>
```

## ✅ Success Criteria

- [x] Reservation data saved to MySQL
- [x] Email sent automatically to admin
- [x] Email contains complete reservation details
- [x] UI remains responsive
- [x] Email failure doesn't break reservation flow

## 🚀 Future Enhancements

- HTML email templates
- Status-based emails (confirmed/rejected)
- Async email sending
- Multiple admin recipients
- Email attachments (reservation PDF)
