# Debugging Log

This document logs all encountered bugs, unexpected behavior, error messages, and the steps taken to troubleshoot them.

---

## Compilation Error: Unresolved Reference `ui`

*   **Symptom:** The Gradle build failed with an `Unresolved reference: ui` error in `MainActivity.kt`.
*   **Troubleshooting:**
    *   Examined `app/src/main/kotlin/com/example/aacarinfo/MainActivity.kt`.
    *   The code was referencing a non-existent package `com.example.aacarinfo.ui.theme`.
    *   The file also contained several unused imports.
*   **Resolution:**
    *   Rewrote `MainActivity.kt` to remove the reference to the non-existent `ui.theme` package.
    *   Removed all unused imports to simplify the code.
    *   The project now compiles successfully.
---

## DHU Script Fails Due to `sudo` Password

*   **Symptom:** The `start_dhu.sh` script fails to start the Desktop Head Unit.
*   **Troubleshooting:**
    *   The script uses `sudo` to run the DHU executable.
    *   The environment does not allow for interactive password entry for `sudo`.
    *   Modified the script to use `sudo -n` to fail fast if a password is required.
*   **Limitation:**
    *   Cannot visually test the application on the DHU due to the `sudo` password requirement.
    *   Will have to rely on compilation and static analysis for now.
---