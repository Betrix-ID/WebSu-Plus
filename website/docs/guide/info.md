# WebSu Plus Documentation

Welcome to the official documentation for **WebSu Plus (WebPlus)**. This platform provides a robust framework for managing and executing web-based system modules on Android devices.

## Authorization Methods

WebSu Plus offers two primary methods for system interaction. Choose the method that best aligns with your device's configuration.

### 1. Root Access (High Privilege)
Recommended for users seeking complete system control and maximum performance.

::: info AUTHORIZATION PROCESS
WebSu Plus is compatible with the following Superuser managers:
* **Magisk**
* **KernelSU**
* **APatch (Sukisu)**

Upon launching the application, grant the requested Superuser permissions when the system prompt appears.
:::

### 2. Shizuku Integration (Non-Root)
A sophisticated solution for users who prefer system-level capabilities without permanent root modifications.

**Deployment Steps:**
1. **Service Initialization:** Ensure the Shizuku service is active. Refer to the [Official Shizuku Setup Guide](https://shizuku.rikka.app/guide/setup/) for detailed instructions.
2. **Application Authorization:**
   * Open the **Shizuku Manager**.
   * Navigate to **Authorized Applications**.
   * Locate **WebSu Plus** and enable the authorization toggle.
3. **Verification:** Return to WebSu Plus to confirm the interface is operational.

## Core Architecture

### Terminal Executor
The platform includes a universal command executor designed for cross-environment compatibility.
* **Intelligent Detection:** Automatically identifies and switches between `root` and `shell` environments.
* **Execution Logs:** Provides real-time feedback for custom script execution.

### WebUI Module Framework
WebSu Plus supports modules developed using standard web technologies.
* **JavaScript API:** Provides a bridge between web modules and Android system APIs.
* **Extensible UI:** Modules maintain full control over their visual presentation while adhering to the platform's execution security.

## Frequently Asked Questions

| Question | Answer |
| :--- | :--- |
| **Security Status** | WebSu Plus is an open-source project, ensuring transparency in all system operations. |
| **Compatibility** | Supports modern Android versions compatible with Shizuku and major Root managers. |
| **Troubleshooting** | If a module fails to execute, verify authorization status and ensure the module is updated to the latest version. |

## Development & Support
WebSu Plus is an evolving ecosystem. We encourage developers to contribute to the codebase, report technical issues, or propose new features to enhance the open Android environment.
