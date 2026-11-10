# Security Policy

The **Rogue TD: Dual-Front** project takes the security and integrity of our codebase, distribution artifacts, and community very seriously.

---

## 🛡️ Supported Versions

We provide security updates and vulnerability patches for the current release and recent minor versions:

| Version | Supported          |
| ------- | ------------------ |
| 0.0.0.006+ | :white_check_mark: |
| 0.0.0.005  | :white_check_mark: |
| < 0.0.0.005 | :x:                |

---

## 🚨 Reporting a Vulnerability

**DO NOT create public GitHub issues or public pull requests for security vulnerabilities.**

If you discover a potential vulnerability, sensitive information exposure, or security defect, please report it privately through one of the following methods:

1. **GitHub Private Vulnerability Reporting (Recommended):**
   - Navigate to the **Security** tab of the repository on GitHub.
   - Click **Report a vulnerability** to open an encrypted advisory draft visible only to maintainers.

2. **Direct Security Contact:**
   - Email our security response team at: **`[security@yourdomain.com]`**
   - If sensitive information or proof-of-concept exploits are included, please request an encrypted PGP communication channel before sending.

---

## ⏱️ Response & Triage Timeline

When a report is received:

- **Initial Acknowledgment:** Within **48 hours**, confirming receipt of your report.
- **Triage & Validation:** Within **5 business days**, detailing vulnerability confirmation, severity classification, and remediation steps.
- **Patch & Advisory Release:** A patched release build will be published along with appropriate credit/attribution to the reporter (unless anonymity is requested).

---

## 🔒 Security Practices for Contributors

- **Zero Hardcoded Secrets:** Never hardcode credentials, private keys, keystore passwords, or proprietary API tokens.
- **Dependency Hygiene:** All dependencies must be audited against known CVEs via automated Dependabot or OWASP scanning.
- **Principle of Least Privilege:** Android permissions declared in `AndroidManifest.xml` must remain minimal and strictly necessary for application functionality.
