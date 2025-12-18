// Opsi konfigurasi default untuk operasi I/O file
const defaultOptions = {
    // Ukuran chunk default untuk membaca file (1 MB)
    chunkSize: 1048576,
    // Header HTTP default
    headers: {
        "Content-Type": "application/octet-stream"
    }
};

/**
 * Mengubah input stream file menjadi ReadableStream.
 * Fungsi ini memungkinkan pembacaan file secara asinkron dalam potongan (chunks).
 * @param {object} inputStream - Objek stream input file (misalnya, dari API lingkungan lokal).
 * @param {object} [options] - Opsi konfigurasi yang menimpa defaultOptions.
 * @returns {Promise<ReadableStream>} Sebuah Promise yang me-resolve ke ReadableStream.
 */
async function wrapToReadableStream(inputStream, options = {}) {
    const config = { ...defaultOptions, ...options };

    return new Promise((resolve, reject) => {
        let fileHandle;
        try {
            // Memastikan input stream valid
            if (!(fileHandle = inputStream)) {
                throw new Error("Failed to open file input stream");
            }
        } catch (error) {
            reject(error);
            return;
        }

        // Handler saat stream dibatalkan (misalnya oleh signal AbortController)
        const abortHandler = () => {
            try {
                fileHandle?.close();
            } catch (cleanupError) {
                console.error("Error during abort cleanup:", cleanupError);
            }
            reject(new DOMException("The operation was aborted.", "AbortError"));
        };

        // Mengatur sinyal abort
        if (config.signal) {
            if (config.signal.aborted) {
                abortHandler();
                return;
            }
            config.signal.addEventListener("abort", abortHandler);
        }

        // Membuat ReadableStream
        const readableStream = new ReadableStream({
            // Dipanggil ketika stream meminta lebih banyak data
            async pull(controller) {
                try {
                    // Membaca chunk dari file
                    const chunk = readChunkFromInput(fileHandle, config.chunkSize);
                    
                    if (!chunk) {
                        // Tidak ada data lagi, tutup stream
                        controller.close();
                        cleanup();
                        return;
                    }
                    controller.enqueue(chunk);
                } catch (pullError) {
                    cleanup();
                    controller.error(pullError);
                    reject(new Error(`Error reading file chunk: ${pullError}`));
                }
            },
            // Dipanggil ketika stream dibatalkan secara eksternal
            cancel(reason) {
                console.warn("Stream canceled:", reason);
                cleanup();
            }
        });

        // Fungsi cleanup: menghapus event listener dan menutup file handle
        function cleanup() {
            try {
                config.signal && config.signal.removeEventListener("abort", abortHandler);
                fileHandle?.close();
            } catch (cleanupError) {
                console.error(`Error during cleanup: ${cleanupError}`);
            }
        }

        resolve(readableStream);
    });
}

/**
 * Membaca satu chunk data dari input stream.
 * Fungsi ini tampaknya menangani berbagai format data dari `readChunk` / `read`
 * (buffer biner, integer tunggal, atau string JSON array).
 * @param {object} inputStream - Objek stream input file.
 * @param {number} [chunkSize] - Ukuran chunk yang akan dibaca.
 * @returns {Uint8Array | null} Chunk data sebagai Uint8Array atau null jika EOF.
 */
function readChunkFromInput(inputStream, chunkSize) {
    try {
        // Memanggil metode readChunk atau read yang disediakan oleh objek input stream
        const data = chunkSize ? inputStream.readChunk(chunkSize) : inputStream.read();
        
        // Menangani berbagai tipe data yang dikembalikan oleh read/readChunk:
        if (typeof data === "number") {
            // Jika mengembalikan integer (mungkin byte tunggal)
            return new Uint8Array([data]);
        }
        
        if (typeof data === "string") {
            // Jika mengembalikan string (diduga stringified JSON array of bytes)
            const jsonArray = JSON.parse(data);
            return (jsonArray && Array.isArray(jsonArray) && jsonArray.length > 0) ? new Uint8Array(jsonArray) : null;
        }

        // Mengembalikan data sebagai Uint8Array jika itu adalah tipe yang diharapkan, atau null
        return data === null ? null : data;
    } catch (error) {
        throw new Error("Error reading chunk data: " + error);
    }
}

/**
 * Mengubah input stream menjadi objek Response standar Fetch API.
 * @param {object} inputStream - Objek stream input file.
 * @param {object} [options] - Opsi konfigurasi Response.
 * @returns {Promise<Response>} Sebuah Promise yang me-resolve ke objek Response.
 */
async function wrapInputStream(inputStream, options = {}) {
    const config = { ...defaultOptions, ...options };
    try {
        const readableStream = await wrapToReadableStream(inputStream, config);
        // Membuat Response baru dari ReadableStream dan konfigurasi header
        return new Response(readableStream, config);
    } catch (error) {
        throw new Error(`wrapInputStream failed: ${error}`);
    }
}

// --- Manajemen Custom Event (WXEventHandler) ---

/**
 * Custom Event class yang menambahkan properti wxOrigin.
 * Berguna untuk membedakan asal event (misalnya "system").
 */
class CustomWXEvent extends CustomEvent {
    _wxOrigin;

    get wxOrigin() {
        return this._wxOrigin;
    }

    set wxOrigin(value) {
        this._wxOrigin = value;
    }

    constructor(type, eventInitDict) {
        // detail: eventInitDict.detail
        super(type, { detail: eventInitDict, bubbles: true, cancelable: true, composed: true });
    }
}

/**
 * Kelas untuk menangani event khusus dari sistem,
 * terutama dari pesan (message) yang diposting ke jendela (window).
 */
class WXEventHandler {
    _initialized = false;
    // Map untuk menyimpan handler yang dibungkus (wrapper)
    _handlers = new WeakMap();
    // Tipe event sistem yang diketahui (melihat prefiks WX, ini mungkin dari WeChat Mini Program/Web View)
    _eventTypes = {
        WX_ON_BACK: "back",
        WX_ON_RESUME: "resume",
        WX_ON_REFRESH: "refresh",
        WX_ON_PAUSE: "pause"
    };

    get eventTypes() {
        return this._eventTypes;
    }

    constructor() {
        if (!this._initialized) {
            this._initialized = true;
            // Mendengarkan semua pesan yang masuk ke window (PostMessage API)
            window.addEventListener("message", event => {
                try {
                    if (typeof event.data !== "string") return;

                    // Mencoba parse data pesan sebagai JSON
                    let eventData = JSON.parse(event.data);
                    if (!eventData?.type) return;

                    // Mendapatkan tipe event yang sesuai
                    let eventName = this._eventTypes[eventData.type] ?? eventData.type;
                    
                    // Meneruskan ke dispatcher
                    this._dispatch(window, eventName, eventData);
                } catch (error) {
                    console.error("[WXEvent] Message error:", error);
                }
            });
        }
    }

    // Mendispatch CustomWXEvent
    _dispatch(target, type, detail) {
        let event = new CustomWXEvent(type, detail);
        event.wxOrigin = "system"; // Menandai event ini berasal dari 'system'
        target.dispatchEvent(event);
    }

    /**
     * Mendaftarkan event listener.
     * @param {EventTarget} target - Target untuk mendengarkan event (misalnya window).
     * @param {string} type - Tipe event.
     * @param {function|object} handler - Fungsi atau objek handler event.
     * @returns {function} Fungsi untuk melepaskan (off) event listener.
     */
    on(target, type, handler) {
        this._initialized || new WXEventHandler(); // Memastikan inisialisasi

        // Fungsi wrapper untuk memfilter event yang hanya berasal dari 'system'
        const wrapper = (event) => {
            if (!(event instanceof CustomWXEvent)) {
                console.warn("[WXEvent] Event is not a CustomWXEvent:", event);
                return;
            }
            
            // Hanya proses event yang ditandai berasal dari 'system'
            if (event.wxOrigin === "system") {
                if (typeof handler === "function") {
                    handler(event);
                } else if (handler && typeof handler.handleEvent === "function") {
                    handler.handleEvent(event);
                }
            }
        };

        // Menyimpan handler asli dan wrapper untuk keperluan 'off'
        if (!this._handlers.has(target)) this._handlers.set(target, new Map());
        let targetHandlers = this._handlers.get(target);

        if (!targetHandlers.has(type)) targetHandlers.set(type, new Set());
        targetHandlers.get(type).add({ handler: handler, wrapper: wrapper });

        // Mendaftarkan event listener ke target
        target.addEventListener(type, wrapper);

        // Mengembalikan fungsi off
        return () => this.off(target, type, handler);
    }

    /**
     * Melepaskan event listener.
     */
    off(target, type, handler) {
        let targetHandlers = this._handlers.get(target);
        if (targetHandlers?.has(type)) {
            for (let item of targetHandlers.get(type)) {
                if (item.handler === handler) {
                    target.removeEventListener(type, item.wrapper);
                    targetHandlers.get(type).delete(item);
                    break;
                }
            }
        }
    }
}

export {
    CustomWXEvent,
    WXEventHandler,
    wrapInputStream,
    wrapToReadableStream
};
