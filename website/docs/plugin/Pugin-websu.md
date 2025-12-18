# WebSu Plugin Architecture (Root & Non-Root)

::: info
WebSu Plugins provide a unified framework that bridges the gap between high-privilege Root environments and restricted Non-Root (Shizuku/ADB) environments.
:::

## Overview

The WebSu Plugin system is engineered upon the established architecture of traditional Root WebUI modules. Rather than serving as a replacement, it functions as an evolutionary extension for the **WebSu Plus** ecosystem. 

This framework allows WebUI plugins to operate seamlessly without requiring root access by leveraging a modular integration layer. This approach ensures a consistent developer experience, allowing the same codebase to balance functionality across rooted and unrooted device states.

## Technical Specifications & Integration

### JavaScript API Support
WebSu Plugins feature a near-universal JavaScript API integration. This includes support for both online and offline KernelSU environments. The API operates atop a robust abstraction layer that intelligently routes commands through either **ADB Shell** or **Root** interfaces based on the available authorization.

### BusyBox Environment
The platform includes a comprehensive BusyBox binary available for both root and non-root users.
* **Binary Path:** `/data/user_de/0/com.android.shell/WebSu/sbin/busybox`
* **Standalone Shell Mode:** Supports `ASH` Standalone Mode, which can be toggled at runtime. When active, commands (e.g., `ls`, `rm`, `chmod`) will prioritize internal BusyBox applets over default Android system binaries (Toybox). This ensures environment predictability across various Android versions and OEM distributions.

## Directory Structure

The standard deployment path for plugins is located at `/data/user_de/0/com.android.shell/WebSu/webui`. Modules must adhere to the following hierarchy:

```text
/data/user_de/0/com.android.shell/WebSu/webui
└── $MOPATH/                 <-- Named after the unique Module ID
    ├── module.prop          <-- Mandatory metadata file
    ├── Amber.sh             <-- Optional customization script (Replaces customize.sh)
    ├── lossy.sh             <-- Late-start boot service script (Replaces service.sh)
    ├── uninstall.sh         <-- Cleanup script executed upon module removal
    ├── webroot/
    │   └── index.html       <-- Primary WebUI entry point
    └── system/
        └── sbin/            <-- Directory for executable binary tools

Metadata Configuration (module.prop)
The module.prop file is the primary identification document for a module. Modules missing this file will not be recognized by the system.
Required Schema:
id=<string>
name=<string>
version=<string>
versionCode=<int>
author=<string>
description=<string>

 * id: Must follow the regex ^[a-zA-Z][a-zA-Z0-9._-]+$. This unique identifier must remain constant once the module is published.
 * versionCode: Must be an integer for version comparison logic.
 * Encoding: Must use UNIX (LF) line endings. Windows (CR+LF) or Macintosh (CR) formats will cause execution failures.
Plugin Installation & Deployment
WebSu Plus plugins are packaged as standard ZIP archives. The installer logic follows the Root Module convention, ensuring compatibility with existing developer workflows while enabling deployment via the WebSu Plus interface.
Core Installation Functions
The following internal functions are available during the installation process:
| Function | Description |
|---|---|
| ui_print <msg> | Outputs a message to the installation console. Use this instead of echo. |
| abort <msg> | Terminals the installation and displays an error message. Ensures proper cleanup. |
| set_perm <target> <owner> <grp> <perm> [ctx] | Sets ownership, permissions, and SELinux context for a specific file. |
| set_perm_recursive <dir> <own> <grp> <dper> <fper> [ctx] | Recursively applies permissions to a directory and its contents. |
