# 🚫 NETWORK CONNECTIVITY TROUBLESHOOTING GUIDE

## Problem
```
java.net.ConnectException: Connection timed out: connect
```

## Root Cause
Network firewall or ISP is blocking SMTP connections to Gmail servers.

## ✅ SOLUTIONS: Network Connectivity Fixes

### Solution 1: Check Network Restrictions
**Most Common Cause**: University/company networks block SMTP ports.

#### Test Current Network:
1. **Try different network**:
   - Switch to mobile hotspot
   - Use home Wi-Fi
   - Try public Wi-Fi (coffee shop)

2. **If it works on different network**: Your current network blocks SMTP

#### Solutions for Restricted Networks:
- **Use mobile hotspot** (most reliable)
- **Contact IT department** to allow SMTP ports 587/465
- **Use VPN** that allows SMTP traffic
- **Use alternative email service** (not Gmail)

### Solution 2: Firewall Check
**Windows Firewall** might block Java applications.

#### Allow Java through Firewall:
1. Open **Windows Defender Firewall**
2. Click **"Allow an app through firewall"**
3. Find **"Java(TM) Platform SE binary"**
4. Check both **Private** and **Public** networks
5. Click **OK**

### Solution 3: Antivirus Software
Some antivirus programs block SMTP connections.

#### Temporarily disable antivirus:
1. **Disable real-time protection**
2. **Test email sending**
3. **Re-enable antivirus**

### Solution 4: ISP Blocking
Some ISPs block port 587/465 to prevent spam.

#### Test with different ISP:
- Use mobile data
- Test from different location

## 🔧 Technical Improvements Added

### Enhanced SMTP Configuration:
```java
// Primary: STARTTLS on port 587
props.put("mail.smtp.starttls.enable", "true");
props.put("mail.smtp.port", "587");

// Fallback: SSL on port 465
props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
props.put("mail.smtp.port", "465");
```

### Connection Timeouts:
```java
props.put("mail.smtp.connectiontimeout", "10000");  // 10 seconds
props.put("mail.smtp.timeout", "10000");
props.put("mail.smtp.writetimeout", "10000");
```

### Automatic Fallback:
- **First try**: STARTTLS (port 587)
- **If fails**: Automatically try SSL (port 465)
- **Clear logging** for which method worked

## 📊 Error Messages & Solutions

### "Connection timed out: connect"
```
🚫 Erreur réseau SMTP: Connexion impossible à smtp.gmail.com
Cause probable: Firewall, réseau universitaire, ou ISP bloque le port SMTP
Solution: Vérifier les paramètres réseau ou utiliser une connexion différente
```

**Solutions**:
- Use mobile hotspot
- Check firewall settings
- Contact network administrator

### "Connection refused"
```
Port 587/465 is blocked by firewall
```

**Solutions**:
- Configure firewall to allow outbound connections on ports 587/465
- Use different network

### "Network is unreachable"
```
DNS resolution failed or network down
```

**Solutions**:
- Check internet connection
- Try different DNS servers

## 🧪 Testing Network Connectivity

### Test SMTP Connection Manually:
```bash
# Test port 587 (STARTTLS)
telnet smtp.gmail.com 587

# Test port 465 (SSL)
openssl s_client -connect smtp.gmail.com:465
```

### Java Network Test:
```java
// Add this to debug network issues
System.setProperty("mail.debug", "true");
```

## 📋 Complete Troubleshooting Checklist

- [ ] **Try mobile hotspot** (bypasses network restrictions)
- [ ] **Check Windows Firewall** (allow Java)
- [ ] **Disable antivirus temporarily**
- [ ] **Test from different location/network**
- [ ] **Contact IT/network administrator**
- [ ] **Check ISP policies**

## ✅ Expected Results

### If Network Allows SMTP:
```
✓ Email de confirmation envoyé pour la réservation #X
```

### If Network Blocks SMTP:
```
❌ Échec d'envoi d'email pour la réservation #X
Raison possible: Connexion réseau bloquée (firewall/université/ISP)
Solutions: Vérifier firewall ou utiliser hotspot mobile
ℹ️ La réservation a été enregistrée malgré l'échec d'email
```

## 🔄 Fallback Behavior

- **Reservation always saves** (network issues don't break core functionality)
- **Email attempts with fallback configurations**
- **Clear error messages** guide user to solutions
- **Application continues working** normally

## 📞 When to Contact Support

### Network Administrator:
- If using university/company network
- Need SMTP ports unblocked

### ISP Support:
- If home internet blocks SMTP
- Need port restrictions removed

### Application Developer:
- If all networks fail (rare)
- If error messages are unclear

## 🎯 Summary

**Network blocking is the most common cause** of SMTP connection failures. The enhanced EmailService now:

- ✅ **Automatically tries multiple SMTP configurations**
- ✅ **Provides clear error messages and solutions**
- ✅ **Never breaks reservation functionality**
- ✅ **Guides users to working solutions**

Try a mobile hotspot first - it usually works! 📱
