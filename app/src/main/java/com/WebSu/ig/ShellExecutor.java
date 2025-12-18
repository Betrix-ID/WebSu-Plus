package com.WebSu.ig;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.*;
import rikka.shizuku.Shizuku;
import com.scottyab.rootbeer.RootBeer; 
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShellExecutor {

        private static final String TAG = "ShellExecutor";

        public static class Result {
                public final int exitCode;
                public final String stdout;
                public final String stderr;
                public final long durationMs;

                public Result(int exitCode, String stdout, String stderr, long durationMs) {
                        this.exitCode = exitCode;
                        this.stdout = stdout == null ? "" : stdout;
                        this.stderr = stderr == null ? "" : stderr;
                        this.durationMs = durationMs;
                    }

                public boolean isSuccess() {
                        return exitCode == 0;
                    }

                @Override
                public String toString() {
                        return "ExitCode: " + exitCode + ", Duration: " + durationMs + "ms\n"
                            + "Stdout:\n" + stdout + "\n"
                            + "Stderr:\n" + stderr;
                    }
            }

        private static final Map<String, Process> SPAWN_MAP =
        Collections.synchronizedMap(new HashMap<String, Process>());

        private static Boolean cachedHasRoot = null; 
        private static Boolean cachedHasShizuku = null;

        public static void invalidateCache() {
                cachedHasRoot = null;
                cachedHasShizuku = null;
                Log.d(TAG, "Shell access cache has been invalidated.");
            }

        public static boolean hasShizuku() {
                if (cachedHasShizuku != null) return cachedHasShizuku;
                try {
                        cachedHasShizuku = Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0;
                    } catch (Throwable t) {
                        cachedHasShizuku = false;
                    }
                Log.d(TAG, "Has Shizuku: " + cachedHasShizuku);
                return cachedHasShizuku;
            }

        public static boolean hasRoot(Context context) {
                if (cachedHasRoot != null) return cachedHasRoot;
                if (context == null) return false; 

                cachedHasRoot = hasRootInternal(context);
                Log.d(TAG, "Has Root (via RootBeer + Sanity Check): " + cachedHasRoot);
                return cachedHasRoot;
            }

        public static boolean hasRoot() {
                if (cachedHasRoot != null) return cachedHasRoot;
                return false; 
            }

        private static boolean hasRootInternal(Context context) {
                if (context == null) return false;
                RootBeer rootBeer = new RootBeer(context);
                if (!rootBeer.isRooted()) {
                        Log.d(TAG, "RootBeer check: NOT rooted.");
                        return false;
                    }

                Log.d(TAG, "RootBeer check: POSSIBLY rooted. Performing sanity check...");

                Process p = null;
                BufferedReader r = null;
                try {
                        p = new ProcessBuilder("su", "-c", "echo ok").start();
                        r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String line = r.readLine();
                        int exitCode = p.waitFor();

                        boolean actualRootAccess = (exitCode == 0 && "ok".equals(line != null ? line.trim() : null));
                        Log.d(TAG, "Sanity Check Exit Code: " + exitCode + ", Output: " + line);
                        return actualRootAccess;

                    } catch (Throwable ignored) {
                        Log.e(TAG, "Sanity Check failed.", ignored);
                        return false;
                    } finally {
                        closeQuietly(r);
                        if (p != null) p.destroy();
                    }
            }

        private static final Pattern ECHO_SINGLE_QUOTE_PATTERN = 
        Pattern.compile("^\\s*echo\\s+'(.+?)'\\s*$", Pattern.DOTALL);

        private static Process buildProcess(String cmd) throws IOException {
                if (cachedHasShizuku == null) hasShizuku();

                String finalCmd = cmd;
                Matcher matcher = ECHO_SINGLE_QUOTE_PATTERN.matcher(finalCmd);

                if (matcher.matches()) {
                        String content = matcher.group(1);
                        content = content.trim();
                        finalCmd = "echo \"" + content.replace("\"", "\\\"") + "\"";
                        Log.d(TAG, "Adjusted command for $() evaluation: " + finalCmd);
                    }

                if (cachedHasShizuku != null && cachedHasShizuku) {
                        try {
                                return Shizuku.newProcess(new String[]{"sh", "-c", finalCmd}, null, null);
                            } catch (Throwable t) {
                                Log.e(TAG, "Shizuku failed, fallback...", t);
                                cachedHasShizuku = false; 
                            }
                    }
                if (cachedHasRoot != null && cachedHasRoot) {
                        return new ProcessBuilder("su", "-c", finalCmd).start();
                    }
                return new ProcessBuilder("sh", "-c", finalCmd).start();
            }

        public static Process execProcess(String cmd, Context ctx) throws IOException {
                return buildProcess(cmd);
            }

        public static Result execSync(String cmd, Context ctx) {
                return execSync(cmd);
            }

        public static Result execSync(String cmd) {
                long start = System.currentTimeMillis();
                Process p = null;
                String out = "";
                String err = "";

                try {
                        p = buildProcess(cmd); 

                        int exit = p.waitFor();
                        out = readStream(p.getInputStream());
                        err = readStream(p.getErrorStream());

                        long dur = System.currentTimeMillis() - start;
                        return new Result(exit, out, err, dur);
                    } catch (Throwable t) {
                        StringWriter sw = new StringWriter();
                        t.printStackTrace(new PrintWriter(sw));
                        long dur = System.currentTimeMillis() - start;
                        Log.e(TAG, "execSync failed for command: " + cmd, t);
                        return new Result(-1, out, sw.toString(), dur);
                    } finally {
                        if (p != null) p.destroy();
                    }
            }

        public static boolean spawnStart(final String id, final String cmd) {
                if (id == null || cmd == null || SPAWN_MAP.containsKey(id)) return false;
                try {
                        final Process p = buildProcess(cmd); 
                        SPAWN_MAP.put(id, p);

                        new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                            BufferedReader br = null;
                                            try {
                                                    br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                                                    String line;
                                                    while ((line = br.readLine()) != null) {
                                                            Log.v(TAG, "[" + id + "] " + line);
                                                        }
                                                } catch (Throwable ignored) {
                                                    Log.e(TAG, "Spawn input stream error for ID: " + id, ignored);
                                                } finally {
                                                    closeQuietly(br);
                                                    try { p.waitFor(); } catch (Throwable ignored) {}
                                                    SPAWN_MAP.remove(id); 
                                                    try { p.destroy(); } catch (Throwable ignored) {}
                                                    Log.d(TAG, "Spawn process finished/destroyed for ID: " + id);
                                                }
                                        }
                                }).start();
                        return true;
                    } catch (Throwable t) {
                        Log.e(TAG, "Failed to spawn process for ID: " + id, t);
                        return false;
                    }
            }

        public static boolean spawnKill(String id) {
                Process p = SPAWN_MAP.remove(id);
                if (p == null) return false;
                try {
                        p.destroy();
                        return true;
                    } catch (Throwable t) {
                        return false;
                    }
            }

        public static String readFile(String path) {
                File file = new File(path);
                if (file.exists() && file.canRead()) {
                        StringBuilder sb = new StringBuilder();
                        BufferedReader br = null;
                        try {
                                br = new BufferedReader(new FileReader(file));
                                String line;
                                while ((line = br.readLine()) != null) {
                                        sb.append(line).append("\n");
                                    }
                                return sb.toString().trim();
                            } catch (IOException e) {
                                Log.w(TAG, "Java text read failed, trying shell: " + e.getMessage());
                            } finally {
                                closeQuietly(br);
                            }
                    }
                String safePath = escapeShellArg(path);
                Result res = execSync("cat " + safePath); 
                if (res.isSuccess()) {
                        return res.stdout.trim();
                    } else {
                        Log.e(TAG, "ReadFile Failed: " + path + " | " + res.stderr);
                        return null;
                    }
            }

        public static byte[] readFileBytes(String path) {
                File file = new File(path);
                if (file.exists() && file.canRead() && file.length() > 0) {
                        FileInputStream fis = null;
                        try {
                                fis = new FileInputStream(file);
                                return readStreamBytes(fis);
                            } catch (IOException e) {
                                Log.w(TAG, "Java binary read failed, trying shell: " + e.getMessage());
                            } finally {
                                closeQuietly(fis);
                            }
                    }

                String safePath = escapeShellArg(path);
                Process p = null;
                try {
                        p = buildProcess("cat " + safePath); 

                        byte[] data = readStreamBytes(p.getInputStream()); 
                        int exitCode = p.waitFor();

                        if (exitCode == 0 && data != null && data.length > 0) {
                                return data;
                            } else {
                                Log.e(TAG, "Shell binary read failed. Exit Code: " + exitCode + ", Error: " + readStream(p.getErrorStream()));
                                return null;
                            }
                    } catch (Throwable t) {
                        Log.e(TAG, "Shell binary read exception for: " + path, t);
                        return null;
                    } finally {
                        if (p != null) p.destroy();
                    }
            }

        public static boolean writeFile(Context context, String path, String content) {
                File cacheFile = new File(context.getCacheDir(), "temp_write_" + System.currentTimeMillis());

                FileWriter writer = null;
                try {
                        writer = new FileWriter(cacheFile);
                        writer.write(content);
                        writer.flush();
                    } catch (IOException e) {
                        Log.e(TAG, "Gagal menulis temp file", e);
                        return false;
                    } finally {
                        closeQuietly(writer);
                    }

                String safeSrc = escapeShellArg(cacheFile.getAbsolutePath());
                String safeDst = escapeShellArg(path);
                String cmd = String.format("cp -f %s %s && chmod 644 %s && rm %s", 
                                           safeSrc, safeDst, safeDst, safeSrc);

                Result res = execSync(cmd); 

                if (!res.isSuccess()) {
                        cacheFile.delete();
                        Log.e(TAG, "WriteFile Failed via Shell: " + res.stderr);
                    }

                return res.isSuccess();
            }

        public static boolean copyFile(String sourcePath, String destPath) {
                String safeSrc = escapeShellArg(sourcePath);
                String safeDst = escapeShellArg(destPath);

                String cmd = String.format("cp -r %s %s", safeSrc, safeDst);

                Result res = execSync(cmd);
                return res.isSuccess();
            }

        public static boolean deleteFile(String path) {
                String safePath = escapeShellArg(path);
                String cmd = "rm -rf " + safePath;
                Result res = execSync(cmd);
                return res.isSuccess();
            }

        public static boolean exists(String path) {
                File f = new File(path);
                if (f.exists()) return true; 

                String safePath = escapeShellArg(path);
                Result res = execSync("ls -d " + safePath); 
                return res.isSuccess();
            }

        public static InputStream getFileInputStream(String path) {
                byte[] contentBytes = readFileBytes(path);
                if (contentBytes != null) {
                        return new ByteArrayInputStream(contentBytes);
                    }
                Log.w(TAG, "Gagal memuat file (0 bytes atau error): " + path);
                return null;
            }

        public static String escapeShellCmd(String cmd) {
                if (cmd == null) return "''";
                return "'" + cmd.replace("'", "'\\''") + "'"; 
            }

        public static String escapeShellArg(String s) {
                if (s == null) return "''";
                return "'" + s.replace("'", "'\\''") + "'";
            }

        private static String readStream(InputStream is) throws IOException {
                if (is == null) return "";
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                return sb.toString();
            }

        public static Process execProcessLive(String cmd) throws IOException {
                Process p = buildProcess(cmd);
                Log.d(TAG, "Process created for live execution: " + cmd);
                return p;
            }

        private static byte[] readStreamBytes(InputStream is) throws IOException {
                if (is == null) return null;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384]; 

                while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }

                buffer.flush();
                return buffer.toByteArray();
            }

        private static void closeQuietly(Closeable c) {
                if (c != null) {
                        try {
                                c.close();
                            } catch (Throwable ignored) {}
                    }
            }
    }

