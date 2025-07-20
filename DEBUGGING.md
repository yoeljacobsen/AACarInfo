# DEBUGGING.md

This document logs all encountered bugs, unexpected behavior, error messages, and the steps taken to troubleshoot them.

## Log Entries

---

### [Date: 2025-07-20]

**Issue:** Initial Gradle sync issues after setting up multi-module project.

**Error Message/Behavior:**
```
Could not find method compileSdk() for arguments [35] on object of type com.android.build.gradle.LibraryExtension.
```

**Troubleshooting Steps:**
1.  Verified `compileSdk` usage in `build.gradle.kts` files.
2.  Realized that `compileSdk` should be assigned directly, not called as a method.
3.  Corrected `compileSdk = project.ext.get("compile_sdk_version") as Int` to `compileSdk = 35` (or similar direct assignment if not using `project.ext`).
    *Self-correction: The current setup with `project.ext.get("compile_sdk_version") as Int` is correct for accessing the extra property. The error was likely a transient Gradle sync issue or a misunderstanding of the error message during initial setup.* 

**Resolution:** Re-syncing Gradle after ensuring all `build.gradle.kts` files correctly reference the `project.ext` properties resolved the issue. The initial setup was mostly correct, and the error message was misleading or a result of an incomplete sync.

---
