# Technical Reference: QuickTerminalActivity

`QuickTerminalActivity` is the core execution engine of WebSu Plus, responsible for bridging user input with low-level system processes. It manages terminal emulation, real-time logging, and cross-environment command execution.

## Key Responsibilities

* **Unified Execution:** Orchestrates commands across Root, Shizuku, and BusyBox environments using a centralized `ExecutorService`.
* **Process Management:** Handles the full lifecycle of system processes, including live stream capturing (`STDOUT`/`STDERR`) and manual process termination.
* **High-Performance UI:** Implements a batch-buffering mechanism to update the terminal view every 230ms, preventing UI freezes during high-frequency output.
* **Dynamic Filtering:** Provides a granular filtering system to toggle visibility for commands, outputs, errors, and system tags.

## Architecture Highlights

### 1. Environment Pathing
The activity automatically injects a custom binary path to ensure module compatibility:
`export PATH=/data/user_de/0/com.android.shell/WebSu/system/sbin:$PATH`

### 2. Stream Handling
Output is captured via an asynchronous `StreamReader`. This ensures that `stdout` and `stderr` are processed on separate threads and joined only upon process exit to prevent data race conditions.

### 3. Data Persistence
Terminal buffers are serialized into timestamped `.log` files. This module supports selective logging based on output type (e.g., saving only success output and ignoring errors).

## Technical Specifications

| Component | Logic |
| :--- | :--- |
| **Concurrency** | SingleThreadExecutor |
| **Process Control** | ShellExecutor & BusyBoxExecutor |
| **UI Updates** | RecyclerView with Stable ID logic |
| **Permissions** | Shizuku Binder & Runtime Storage Access |

## System Paths
* **Binaries:** `/data/user_de/0/com.android.shell/WebSu/system/sbin`
* **Workspace:** `/data/user_de/0/com.android.shell/WebSu/webui`
* **Log Storage:** `Internal Storage > Android/data/com.WebSu.ig/files/logs/`
