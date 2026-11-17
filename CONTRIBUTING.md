# Contributing to TimeMachine

Contributions, bug reports, forks, and pull requests are welcome on [GitHub](https://github.com/LiLabsAI/TimeMachine).

## Reporting Issues

- Open an issue describing your Android device model, Android / OEM OS version, steps to reproduce, and expected vs. actual behavior.
- For security-sensitive issues, avoid posting private backup files, signing keys, credentials, or personal data in public issues.

## Development & Verification

1. Fork the repository and create a feature branch (`git checkout -b feature/your-feature`).
2. Keep release signing files (`*.jks`, `*.keystore`, `*.idsig`, `keystore.properties`) local and out of commits.
3. Before opening a pull request, run the Android verification suite:

```bash
# Unit tests, Android Lint, and Debug APK build
./gradlew testDebugUnitTest lintDebug assembleDebug

# Instrumented Room migration & backup tests (with an emulator or ADB device attached)
./gradlew connectedDebugAndroidTest
```

## Licensing & Attribution

By submitting a contribution to **TimeMachine**, you agree that your contribution will be licensed under the **GNU General Public License v3.0 (`GPL-3.0`)** (see [LICENSE](LICENSE)). Please preserve existing copyright and attribution notices as described in [NOTICE](NOTICE).
