# 🚨 GMAIL APP PASSWORD SETUP GUIDE

## Problem
```
535-5.7.8 Username and Password not accepted
jakarta.mail.AuthenticationFailedException
```

## Root Cause
Gmail blocks basic authentication when using normal passwords. You MUST use a Gmail App Password.

## ✅ SOLUTION: Gmail App Password Setup

### Step 1: Enable 2-Step Verification
1. Go to your Gmail account: https://myaccount.google.com/
2. Click **"Security"** in the left sidebar
3. Under **"Signing in to Google"**, click **"2-Step Verification"**
4. Follow the steps to enable 2-Step Verification
5. **IMPORTANT**: Keep this page open - you'll need it for Step 2

### Step 2: Generate App Password
1. On the same **"Security"** page, scroll down to **"App passwords"**
2. Click **"App passwords"**
3. You might need to sign in again
4. Select **"Mail"** as the app
5. Select **"Other (custom name)"** as the device
6. Enter **"CuraVita"** as the custom name
7. Click **"Generate"**
8. **COPY THE 16-CHARACTER PASSWORD** that appears
   - It will look like: `abcd-efgh-ijkl-mnop`
   - **Save this password - you won't see it again!**

### Step 3: Update EmailService.java
1. Open `src/main/java/org/example/service/EmailService.java`
2. Replace the placeholder values:

```java
// BEFORE (will fail):
private final String GMAIL_USERNAME = "your-email@gmail.com";
private final String GMAIL_APP_PASSWORD = "your-app-password";

// AFTER (working):
private final String GMAIL_USERNAME = "ihebjbir57@gmail.com";
private final String GMAIL_APP_PASSWORD = "abcd-efgh-ijkl-mnop"; // Your 16-char App Password
```

### Step 4: Test the Configuration
1. Compile and run the application
2. Create a test reservation
3. Check console for success message:
   ```
   Email de confirmation envoyé pour la réservation #X
   ```
4. Verify email arrives at: `farah.hannachi@esprit.tn`

## 🔧 Technical Details

### SMTP Configuration (Already Correct)
```java
Properties props = new Properties();
props.put("mail.smtp.auth", "true");
props.put("mail.smtp.starttls.enable", "true");
props.put("mail.smtp.host", "smtp.gmail.com");
props.put("mail.smtp.port", "587");
props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
```

### Authentication (Now Fixed)
```java
return new PasswordAuthentication(GMAIL_USERNAME, GMAIL_APP_PASSWORD);
```

## 🚨 Important Rules

- ✅ **DO use** Gmail App Password (16 characters)
- ❌ **DO NOT use** normal Gmail password
- ✅ **DO enable** 2-Step Verification first
- ✅ **DO use** `ihebjbir57@gmail.com` as sender
- ✅ **DO send to** `farah.hannachi@esprit.tn` as recipient

## 🧪 Testing Checklist

- [ ] 2-Step Verification enabled on Gmail
- [ ] App Password generated and saved
- [ ] EmailService.java updated with real credentials
- [ ] Application compiled successfully
- [ ] Test reservation created
- [ ] Email sent successfully (check console)
- [ ] Email received at admin inbox

## 🔍 Troubleshooting

### Still getting authentication error?
1. **Check App Password**: Make sure you copied the full 16-character password
2. **Check Gmail Address**: Ensure `ihebjbir57@gmail.com` is correct
3. **Check 2FA**: Verify 2-Step Verification is enabled
4. **Regenerate**: If issues persist, delete and recreate the App Password

### Email not received?
1. Check spam/junk folder
2. Verify recipient email: `farah.hannachi@esprit.tn`
3. Check Gmail security alerts for blocked sign-ins
4. **If connection timeout occurs**: See NETWORK_CONNECTIVITY_FIX.md

## 📧 Email Content
Subject: `Reservation Confirmation - CuraVita`
Contains: Reservation ID, Client info, Service details, Date/Time, Status

## ✅ Expected Result
- ✅ No `AuthenticationFailedException`
- ✅ Email sends successfully
- ✅ Admin receives reservation notifications
- ✅ Reservation system works normally

