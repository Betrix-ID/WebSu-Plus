package com.WebSu.ig.shell;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashLogHandler implements UncaughtExceptionHandler {

        private final Context context;
        private final UncaughtExceptionHandler defaultUEH;
        private static final String TAG_CRASH = "APP_CRASH_HANDLER";
        private static final String CRASH_LOG_FILENAME = "crash.log";

        public CrashLogHandler(Context context) {
                this.context = context;
                // Simpan handler default agar crash tetap bisa diproses oleh Android
                this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
            }

        /**
         * Metode utama untuk menangani semua Unhandled Exception.
         */
        @Override
        public void uncaughtException(Thread thread, Throwable exception) {

                // Ambil stack trace penuh
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                exception.printStackTrace(pw);
                String stackTrace = sw.toString();

                // Format log yang akan ditulis
                String crashContent = 
                    "FATAL CRASH\n" + 
                    "Thread: " + thread.getName() + 
                    "\nException: " + exception.getClass().getSimpleName() + 
                    "\nMessage: " + exception.getMessage() + 
                    "\n--- STACK TRACE ---\n" + stackTrace;

                // 1. Log ke Logcat Android (Tetap dipertahankan)
                Log.e(TAG_CRASH, "FATAL UNCAUGHT EXCEPTION on thread: " + thread.getName());
                Log.e(TAG_CRASH, stackTrace);

                // 2. Tulis ke file crash.log (Fungsi baru yang digabungkan)
                writeLogToFile(context, CRASH_LOG_FILENAME, crashContent);

                // Panggil handler default Android
                defaultUEH.uncaughtException(thread, exception);
            }

        /**
         * Metode untuk menulis log crash ke file. 
         * Menggantikan LogUtils.writeLog().
         */
        private void writeLogToFile(Context context, String filename, String content) {
                try {
                        // Dapatkan path: getExternalFilesDir(null)/logs/
                        File rootDir = context.getExternalFilesDir(null);

                        // Buat direktori 'logs'
                        File logDir = new File(rootDir, "logs");
                        if (!logDir.exists()) {
                                logDir.mkdirs(); 
                            }

                        // Path file log: .../files/logs/crash.log
                        File logFile = new File(logDir, filename);

                        // Tambahkan timestamp di awal log
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
                        String timestamp = "\n\n[" + sdf.format(new Date()) + "] ";

                        // Gunakan FileWriter dengan 'append' (true)
                        FileWriter writer = new FileWriter(logFile, true);
                        writer.append(timestamp).append(content);
                        writer.flush();
                        writer.close();

                        Log.i(TAG_CRASH, "Crash log successfully written to: " + logFile.getAbsolutePath());

                    } catch (IOException e) {
                        Log.e(TAG_CRASH, "Error writing crash log file: " + e.getMessage());
                    }
            }

        /**
         * Metode statis untuk menginisialisasi handler ini di aplikasi.
         */
        public static void attach(Context context) {
                Thread.setDefaultUncaughtExceptionHandler(new CrashLogHandler(context));
            }
    }

