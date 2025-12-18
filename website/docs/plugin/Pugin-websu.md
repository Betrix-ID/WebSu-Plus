Berikut adalah kode Markdown yang sudah dirapikan dan siap Anda copy-paste langsung ke file Pugin-websu.md:
# WebSu Plugin Architecture (Root & Non-Root)

::: info
WebSu Plugins provide a unified framework that bridges the gap between high-privilege Root environments and restricted Non-Root (Shizuku/ADB) environments.
:::

## Overview

The WebSu Plugin system is engineered upon the established architecture of traditional Root WebUI modules. Rather than serving as a replacement, it functions as an evolutionary extension for the **WebSu Plus** ecosystem. 

This framework allows WebUI plugins to operate seamlessly without requiring root access by leveraging a modular integration layer. This approach ensures a consistent developer experience, allowing the same codebase to balance functionality across rooted and unrooted device states.

## Technical Specifications

### JavaScript API Support
WebSu Plugins feature a near-universal JavaScript API integration, including support for online and offline KernelSU environments. The API operates atop a robust abstraction layer that intelligently routes commands through either **ADB Shell** or **Root** interfaces based on available authorization.

### BusyBox Environment
A comprehensive BusyBox binary is available for all users.
* **Binary Path:** `/data/user_de/0/com.android.shell/WebSu/sbin/busybox`
* **Standalone Shell Mode:** Supports `ASH` Standalone Mode. When active, commands (e.g., `ls`, `rm`, `chmod`) prioritize internal BusyBox applets over default Android binaries (Toybox), ensuring predictability across different Android versions.

## Directory Structure

The standard deployment path is `/data/user_de/0/com.android.shell/WebSu/webui`. Modules must adhere to the following hierarchy:

```
text
/data/user_de/0/com.android.shell/WebSu/webui
└── $MOPATH/                 <-- Unique Module ID
    ├── module.prop          <-- Mandatory metadata
    ├── Amber.sh             <-- Customization script (replaces customize.sh)
    ├── lossy.sh             <-- Boot service script (replaces service.sh)
    ├── uninstall.sh         <-- Cleanup script
    ├── webroot/
    │   └── index.html       <-- WebUI entry point
    └── system/
        └── sbin/            <-- Binary tools directory
```

### Metadata Configuration (module.prop)
::: info
The module.prop file is the primary identification document. Missing files will result in the module not being recognized.
Required Schema
:::

id=<string>
name=<string>
version=<string>
versionCode=<int>
author=<string>
description=<string>

### Constraints
 - ID Format: Must match regex ^[a-zA-Z][a-zA-Z0-9._-]+$.
 - Version Code: Must be an integer for version comparison.
 - Encoding: Must use UNIX (LF) line endings. Windows (CR+LF) will cause execution failures.
 - Installation & Deployment
Plugins are packaged as standard ZIP archives. The installer follows the Root Module convention to ensure compatibility with existing developer workflows.
Core Installation Functions

| Function | Description |
|---|---|
| ui_print <msg> | Outputs a message to the console. |
| abort <msg> | Terminates installation with an error message. |
| set_perm <target> <own> <grp> <perm> [ctx] | Sets file ownership, permissions, and SELinux context. |
| set_perm_recursive <dir> <own> <grp> <dp> <fp> [ctx] | Recursively applies permissions to a directory. |
