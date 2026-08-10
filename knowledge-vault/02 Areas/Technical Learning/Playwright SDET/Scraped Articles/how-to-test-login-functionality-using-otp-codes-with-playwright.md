# How to test login functionality using OTP codes with Playwright

Broken or unreliable login systems can lead to frustrating user experiences, potential security vulnerabilities, and lost trust.

## Why test login functionality?

Automating login tests with Playwright saves time and ensures consistency.

## Why Playwright for login testing with Mailosaur?

- Cross-browser support
- Email verification testing (OTP, 2FA, activation links)
- Fast execution with Mailosaur API
- Automatic waiting in Playwright
- Full page testing with email integration

## Complex login scenarios

Testing 2FA via SMS or email OTP requires handling one-time passcodes and multi-step verification.

### Set up a simple Playwright project

```
npm create mailosaur@latest
```

### OTP via SMS

Use MailosaurClient to retrieve SMS passcode after login attempt, fill OTP field, submit, confirm login.

### OTP via email

Use catch-all Mailosaur email addresses, retrieve email via mailosaur.messages.get(), extract passcode from email.text.codes[0].

