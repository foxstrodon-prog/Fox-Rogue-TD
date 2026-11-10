# Contributing to Rogue TD: Dual-Front

Thank you for your interest in contributing to **Rogue TD: Dual-Front**! We welcome community contributions, bug reports, and feature proposals that adhere to our code quality, safety, and architectural standards.

---

## 📜 Code of Conduct

All contributors and maintainers are expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md). Please read it before participating.

---

## 🔒 Security & Zero Secrets Policy (STRICT)

**CRITICAL REQUIREMENT:**
- **NEVER commit sensitive credentials, API keys, tokens, signing certificates, or personal data.**
- All local configurations must stay in `local.properties` or `.env` (both are git-ignored).
- If your contribution requires credentials or cloud parameters, use `BuildConfig` fields, Gradle properties, or environment variables backed by placeholder definitions in `.env.example` or `local.properties.example`.
- Automated pre-commit hooks and repository secret scanners reject pull requests containing high-entropy tokens or private keys.

---

## 🐛 Reporting Bugs

1. Search existing issues before creating a new report to avoid duplicates.
2. Use the [Bug Report Template](.github/ISSUE_TEMPLATE/bug_report.md).
3. Include:
   - A clear and descriptive title.
   - Exact step-by-step instructions to reproduce the behavior.
   - Expected vs. actual behavior.
   - Device information: Android OS version, device model / emulator configuration, app version (`v0.0.0.00x`).
   - Relevant logcat stack traces or screenshots.

---

## 💡 Suggesting Features & Enhancements

1. Use the [Feature Request Template](.github/ISSUE_TEMPLATE/feature_request.md).
2. Clearly explain the motivation, gameplay value, or architectural benefit.
3. Discuss technical constraints, potential performance impacts (especially on Compose 60 FPS rendering and simulation loops), and alternative approaches.

---

## 🛠️ Development Workflow

### 1. Fork & Branch
- Fork the repository on GitHub.
- Clone your fork locally:
  ```bash
  git clone https://github.com/<your-username>/Fox-Rogue-TD.git
  cd Fox-Rogue-TD
  ```
- Create a dedicated feature branch from `main`:
  ```bash
  git checkout -b feature/your-feature-name
  # Or for bug fixes:
  git checkout -b fix/issue-description
  ```

### 2. Code Standards & Architecture Guidelines
- **Kotlin & Compose Standards**: Write idiomatic, clean Kotlin. Ensure UI components respect Compose recomposition rules (e.g., avoid allocations inside drawing loops).
- **Architecture**: Adhere to Unidirectional Data Flow (UDF) and Model-View-Intent (MVI) patterns. All state modifications flow through `RogueTDViewModel` via immutable `_uiState.update`.
- **Game Engine Separation**: Keep map generation, tower upgrades, balance calculations, and damage models inside `com.example.engine` and `com.example.model` without circular dependencies on UI composables.
- **Resource Management**: Properly scope coroutines and lifecycle observers (e.g. `OrientationEventListener`) to avoid memory or battery leaks.

### 3. Commit Message Conventions
We follow [Conventional Commits](https://www.conventionalcommits.org/):
- `feat: add elemental resistance indicator`
- `fix: resolve floating text mid-frame overwrite`
- `refactor: extract damage calculation logic into DamageCalculator`
- `test: add Euclidean hit-radius targeting assertions`
- `docs: update README with release instructions`

---

## 🧪 Testing Requirements

Every Pull Request must pass all automated tests prior to review:

```bash
# Run unit and Robolectric test suite
./gradlew testDebugUnitTest

# Assemble debug APK to ensure no compilation or layout errors
./gradlew assembleDebug

# Optional: compile portable Windows widescreen runner
./gradlew buildWindowsLauncher
```

- Add unit test coverage for new algorithms, mathematical formulas (range checks, damage matrices), or state transitions in `ExampleRobolectricTest.kt`.
- Ensure tests run cleanly across all supported Android API levels without flakiness.

---

## 🚀 Submitting a Pull Request

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/your-feature-name
   ```
2. Open a Pull Request against the `main` branch of this repository.
3. Complete the [Pull Request Template](.github/PULL_REQUEST_TEMPLATE.md):
   - Reference linked issues (e.g., `Closes #42`).
   - Fill in the self-audit checklist (security, style, tests).
4. Maintainers will review your PR, suggest changes if needed, and merge once CI checks pass.
