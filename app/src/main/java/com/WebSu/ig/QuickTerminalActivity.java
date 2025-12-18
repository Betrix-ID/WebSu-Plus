package com.WebSu.ig;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.text.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import androidx.core.content.*;
import androidx.core.app.ActivityCompat; 
import androidx.recyclerview.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import rikka.shizuku.*;
import java.text.SimpleDateFormat; 
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Gravity;
import com.WebSu.ig.websuCard.FilterSettingsPanel;
import com.WebSu.ig.BusyBoxExecutor;
import com.WebSu.ig.ShellExecutor;
import com.WebSu.ig.updateStatusBarColor;
import com.WebSu.ig.TerminalOutputType; 
import java.lang.Process;
import android.animation.ObjectAnimator; 

public class QuickTerminalActivity extends Activity {

        private static final String PREFS_NAME = "TerminalFilterPrefs";
        private static final String KEY_OUTPUT_PREFIX = "OutputFilter_"; 
        private static final String KEY_LOG_SAVE_PREFIX = "SaveFilter_"; 
        private static final String KEY_VOLUME_PREFIX = "VolumeFilter_";
        private static final String KEY_DELETE_PREFIX = "DeleteFilter_";

        private static final String BLOCKING_VOLUME_UP_KEY = "BLOKING_VOLUME_UP";
        private static final String BLOCKING_VOLUME_DOWN_KEY = "BLOKING_VOLUME_DOWN";

        private static final String DELETE_COMMAND_KEY = TerminalOutputType.TYPE_COMMAND.name(); 
        private static final String DELETE_OUTPUT_KEY = TerminalOutputType.TYPE_STDOUT.name();   
        private static final String DELETE_INPUT_KEY = TerminalOutputType.TYPE_STDIN.name();     

        private static final String COMMAND_SHELL_TAG = "[commandShell] -> ";
        private static final String COMMAND_ROOT_TAG = "[commandRoot] -> ";
        private static final String COMMAND_SHIZUKU_TAG = "[commandShizuku] -> "; 
        private static final String COMMAND_BUSYBOX_TAG = "[busyboxShell] -> ";
        private static final String COMMAND_BUSYBOX_ROOT_TAG = "[busyboxRoot] -> "; 
        private static final String START_TAG = "[start] pid=";
        private static final String EXIT_TAG = "[exit] code=";
        private static final String THROW_TAG = "[throw] ";
        private static final String STOPPED_TAG = "[stopped]";
        private static final String CANCELLED_TAG = "[Cancelled]";

        public static class TerminalLine {
                public final String text;
                public final TerminalOutputType type;
                public TerminalLine(String text, TerminalOutputType type) {
                        this.text = text;
                        this.type = type;
                    }
            }

        private EditText inputCommand;
        private Button btnRunCommand;
        private ImageButton btnClearOutput;
        private ImageButton btnStopLoping;
        private ImageButton btnSaveAllOutput; 
        private ImageButton btnSelectFilter; 

        private RecyclerView outputRecyclerView;
        private TerminalOutputAdapter adapter; 
        private LinearLayoutManager layoutManager;
        private final List<TerminalLine> outputDataList = new ArrayList<TerminalLine>(); 

        private Handler uiHandler;

        private ExecutorService executor;
        private static final int REQUEST_CODE_SHIZUKU = 8888;

        private static final int PERMISSION_REQUEST_CODE = 100;
        private static final String LOG_DIRECTORY = "logs"; 

        private static final long UI_UPDATE_INTERVAL_MS = 230;
        private static final long SCROLL_DELAY_MS = 90;

        private static final String BUSYBOX_TAG = "[Busybox]";
        private static final String TARGET_NAME = "busybox";
        private final ArrayList<Runnable> pendingUpdateRunnables = new ArrayList<Runnable>();
        private boolean isScrollScheduled = false;
        private volatile Process currentProcess = null; 

        private static final int FROZEN_COLOR = 0xB7B6AFAF; 
        private int ACTIVE_COLOR; 
        private int ERROR_COLOR; 

        private boolean isSaveButtonVisible = true;
        private ObjectAnimator showAnimator;
        private ObjectAnimator hideAnimator;
        private final int ANIMATION_DURATION = 300;

        @Override
        protected void onCreate(Bundle savedInstanceState) {

                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_quick_terminal);
                ACTIVE_COLOR = ContextCompat.getColor(this, R.color.colorOnPrimary);
                ERROR_COLOR = ContextCompat.getColor(this, R.color.colorOnPrimary);

                BusyBoxExecutor.initializePath(this); 
                updateStatusBarColor.updateStatusBarColor(this);
                ShellExecutor.hasRoot(this); 
                ShellExecutor.hasShizuku(); 

                uiHandler = new Handler(Looper.getMainLooper());

                inputCommand = findViewById(R.id.input_command);
                btnRunCommand = findViewById(R.id.btn_run_command);

                btnClearOutput = findViewById(R.id.btn_clear_output);
                btnStopLoping = findViewById(R.id.stop_loping); 
                btnSaveAllOutput = findViewById(R.id.btn_save_all_output); 
                btnSelectFilter = findViewById(R.id.select_filter);

                outputRecyclerView = findViewById(R.id.output_recycler_view);

                layoutManager = new LinearLayoutManager(this);
                outputRecyclerView.setLayoutManager(layoutManager);
                outputRecyclerView.setHasFixedSize(true);
                adapter = new TerminalOutputAdapter(outputDataList, this); 
                outputRecyclerView.setAdapter(adapter);
                executor = Executors.newSingleThreadExecutor();

                btnSaveAllOutput.post(new Runnable() {
                            @Override
                            public void run() {
                                    final float translationY = btnSaveAllOutput.getHeight() + 50; 

                                    showAnimator = ObjectAnimator.ofFloat(btnSaveAllOutput, "translationY", translationY, 0);
                                    showAnimator.setDuration(ANIMATION_DURATION);

                                    hideAnimator = ObjectAnimator.ofFloat(btnSaveAllOutput, "translationY", 0, translationY);
                                    hideAnimator.setDuration(ANIMATION_DURATION);

                                    btnSaveAllOutput.setTranslationY(translationY);
                                    btnSaveAllOutput.setVisibility(View.GONE); 
                                    isSaveButtonVisible = false;
                                    checkSaveButtonDataVisibility();
                                }
                        });

                outputRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                            @Override
                            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                                    super.onScrolled(recyclerView, dx, dy);

                                    if (outputDataList.isEmpty()) return;

                                    if (dy > 0 && isSaveButtonVisible) {
                                            hideSaveButton();
                                        } 
                                    else if (dy < 0 && !isSaveButtonVisible) {
                                            showSaveButton();
                                        }
                                }

                            @Override
                            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                                    super.onScrollStateChanged(recyclerView, newState);
                                    if (outputDataList.isEmpty()) return;

                                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                            if (layoutManager.findFirstCompletelyVisibleItemPosition() == 0 || 
                                                layoutManager.findLastCompletelyVisibleItemPosition() == outputRecyclerView.getAdapter().getItemCount() - 1) {
                                                    showSaveButton();
                                                }
                                        }
                                }
                        });

                try {
                        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU);
                            }
                    } catch (Throwable ignored) {}

                resetButtonState();

                btnStopLoping.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    stopCurrentProcess();
                                }
                        });

                btnRunCommand.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    final String command = inputCommand.getText().toString().trim();
                                    if (TextUtils.isEmpty(command)) {
                                            return;
                                        }
                                    if (currentProcess != null) {
                                            appendOutput(STOPPED_TAG, TerminalOutputType.TYPE_THROW, true); 
                                            return;
                                        }

                                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                                    if (imm != null && getCurrentFocus() != null) {
                                            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                                        }

                                    final boolean deleteOutputActive = isDeleteFilterEnabled(DELETE_OUTPUT_KEY);
                                    if (deleteOutputActive) {
                                            appendOutput("", TerminalOutputType.TYPE_SPACE, false); 
                                        } else {
                                            clearOutput(false); 
                                        }

                                    btnRunCommand.setEnabled(false);
                                    btnStopLoping.setEnabled(true); 
                                    btnStopLoping.setColorFilter(ACTIVE_COLOR, PorterDuff.Mode.SRC_IN);

                                    for (Runnable r : pendingUpdateRunnables) {
                                            uiHandler.removeCallbacks(r);
                                        }
                                    pendingUpdateRunnables.clear();

                                    final int pid = (int) (Math.random() * 100000);
                                    final String safeCmd = command.trim();
                                    final String tag;

                                    if (safeCmd.equals("bb") || safeCmd.equals("bb:") || safeCmd.startsWith("bb ") || safeCmd.startsWith("bb:")) {
                                            tag = BUSYBOX_TAG;
                                        } else {
                                            tag = ""; 
                                        }

                                    final String pidStartMsg = START_TAG + pid;
                                    appendOutput(pidStartMsg, TerminalOutputType.TYPE_START, true);

                                    executor.submit(new Runnable() {
                                                @Override
                                                public void run() {
                                                        Process process = null;
                                                        String finalCmd = null; 
                                                        String[] finalArgs = null;
                                                        boolean useBusyBoxExecutor = false;
                                                        final String busyboxPath = BusyBoxExecutor.getPath();                                                     
                                                        String currentCommandTag; 

                                                        final boolean isBusyBoxCommand = tag.equals(BUSYBOX_TAG);
                                                        ShellExecutor.hasRoot(getApplicationContext());
                                                        ShellExecutor.hasShizuku(); 

                                                        final boolean isShizukuActive = ShellExecutor.hasShizuku();
                                                        final boolean isRootActive = ShellExecutor.hasRoot(); 

                                                        try {
                                                                if (isBusyBoxCommand) {
                                                                        final String cut;
                                                                        currentCommandTag = (isRootActive || isShizukuActive) ? COMMAND_BUSYBOX_ROOT_TAG : COMMAND_BUSYBOX_TAG; 

                                                                        if (safeCmd.equals("bb") || safeCmd.equals("bb:")) {
                                                                                finalCmd = TARGET_NAME;
                                                                                finalArgs = new String[]{TARGET_NAME};
                                                                                useBusyBoxExecutor = true; 
                                                                            } else if (safeCmd.startsWith("bb:")) {
                                                                                cut = safeCmd.substring(3).trim();
                                                                                finalCmd = busyboxPath + " " + cut; 
                                                                                finalArgs = splitArgs(cut);
                                                                                useBusyBoxExecutor = false;
                                                                            } else { // safeCmd.startsWith("bb ");
                                                                                cut = safeCmd.substring(safeCmd.indexOf(' ') + 1).trim();
                                                                                finalCmd = busyboxPath + " " + cut; 
                                                                                finalArgs = splitArgs(cut);
                                                                                useBusyBoxExecutor = false;
                                                                            }

                                                                        if (useBusyBoxExecutor) {
                                                                                final String result = BusyBoxExecutor.run(finalArgs, getApplicationContext()); 
                                                                                final String out = result != null ? result : "";

                                                                                final String commandTagForUiThread = currentCommandTag; 

                                                                                runOnUiThread(new Runnable() {
                                                                                            @Override
                                                                                            public void run() {
                                                                                                    appendOutput(commandTagForUiThread + safeCmd, TerminalOutputType.TYPE_COMMAND, false);
                                                                                                    if (out.length() > 0) {
                                                                                                            String[] lines = out.split("\\r?\\n");
                                                                                                            for (final String line : lines) { 
                                                                                                                    appendOutput(line, TerminalOutputType.TYPE_STDOUT, false);
                                                                                                                }
                                                                                                        }
                                                                                                    appendOutput(EXIT_TAG + 0, TerminalOutputType.TYPE_EXIT, true);
                                                                                                    resetButtonState();
                                                                                                }
                                                                                        });
                                                                                return;
                                                                            }

                                                                        if (TextUtils.isEmpty(busyboxPath)) {
                                                                                throw new IOException("Busybox not initialized. Path is null.");
                                                                            }
                                                                        finalCmd = finalCmd == null ? safeCmd : finalCmd;

                                                                    } else { 
                                                                        String tempCmd = safeCmd;
                                                                        if (tempCmd.endsWith(".sh") && tempCmd.startsWith("/sdcard")) {
                                                                                tempCmd = "sh " + tempCmd;
                                                                            }
                                                                        finalCmd = tempCmd;

                                                                        if (isShizukuActive) {
                                                                                currentCommandTag = COMMAND_SHELL_TAG; 
                                                                            } else if (isRootActive) {
                                                                                currentCommandTag = COMMAND_ROOT_TAG; 
                                                                            } else {
                                                                                currentCommandTag = COMMAND_SHELL_TAG; 
                                                                            }
                                                                    }

                                                                final String finalCommandToExecute = finalCmd;
                                                                final String sbinPath = "/data/user_de/0/com.android.shell/WebSu/system/sbin";
                                                                final String ChmodDir = " /data/user_de/0/com.android.shell/WebSu";
                                                                final String commandWithSetup = 
                                                                    "export PATH=" + sbinPath + ":$PATH && " +
                                                                    "chmod -R 777 " + sbinPath + " 2>/dev/null && " + 
                                                                    "chmod -R 777 " + ChmodDir + " 2>/dev/null && " +
                                                                    finalCommandToExecute; 

                                                                final String commandTagForUiThread = currentCommandTag; 
                                                                runOnUiThread(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                    appendOutput(commandTagForUiThread + finalCommandToExecute, TerminalOutputType.TYPE_COMMAND, false);
                                                                                }
                                                                        });

                                                                process = ShellExecutor.execProcessLive(commandWithSetup);
                                                                currentProcess = process;

                                                                if (process != null) {
                                                                        runProcessStreams(process, String.valueOf(pid)); 
                                                                    } else {
                                                                        throw new IOException("Failed to create process using ShellExecutor.");
                                                                    }

                                                            } catch (final Exception e) {
                                                                runOnUiThread(new Runnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                    appendOutput(THROW_TAG + e.getMessage(), TerminalOutputType.TYPE_THROW, true);
                                                                                    appendOutput(EXIT_TAG + -1, TerminalOutputType.TYPE_EXIT, true); 
                                                                                    resetButtonState(); 
                                                                                }
                                                                        });
                                                            } 
                                                    }
                                            });
                                }
                        });

                btnClearOutput.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    final boolean deleteCommandActive = isDeleteFilterEnabled(DELETE_COMMAND_KEY);
                                    final boolean deleteInputActive = isDeleteFilterEnabled(DELETE_INPUT_KEY);

                                    if (deleteInputActive) {
                                            clearOutputFiltered(); 
                                            inputCommand.setText(""); 

                                        } else if (deleteCommandActive) {
                                            clearOutput(false); 

                                        } else {
                                            clearOutput(true); 
                                        }
                                }
                        });

                btnSaveAllOutput.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    if (checkStoragePermission()) {
                                            saveLogToFile();
                                        }
                                }
                        });

                btnSelectFilter.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                    final FilterSettingsPanel filterPanel = new FilterSettingsPanel(QuickTerminalActivity.this);
                                    filterPanel.setOnDismissListener(new FilterSettingsPanel.OnDismissListener() {
                                                @Override
                                                public void onPanelDismissed() {
                                                        if (adapter != null) {
                                                                adapter.rebuildFilter();
                                                            }
                                                    }
                                            });

                                    filterPanel.show();
                                }
                        });
            }

        private void checkSaveButtonDataVisibility() {
                if (outputDataList.isEmpty()) {
                        isSaveButtonVisible = false;
                        btnSaveAllOutput.setVisibility(View.GONE);
                        if (showAnimator != null && showAnimator.isRunning()) showAnimator.cancel();
                        if (hideAnimator != null && hideAnimator.isRunning()) hideAnimator.cancel();
                    } else {
                        if (!isSaveButtonVisible) {
                                showSaveButton();
                            }
                    }
            }

        private void showSaveButton() {
                if (isSaveButtonVisible) return;
                if (outputDataList.isEmpty()) return;

                if (hideAnimator != null && hideAnimator.isRunning()) {
                        hideAnimator.cancel();
                    }

                if (showAnimator != null) {
                        btnSaveAllOutput.setVisibility(View.VISIBLE);
                        showAnimator.start();
                        isSaveButtonVisible = true;
                    } else {
                        btnSaveAllOutput.setVisibility(View.VISIBLE);
                        isSaveButtonVisible = true;
                    }
            }

        private void hideSaveButton() {
                if (!isSaveButtonVisible) return;

                if (showAnimator != null && showAnimator.isRunning()) {
                        showAnimator.cancel();
                    }

                if (hideAnimator != null) {
                        hideAnimator.start();
                        uiHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                            if (!isSaveButtonVisible) { 
                                                    btnSaveAllOutput.setVisibility(View.GONE);
                                                }
                                        }
                                }, ANIMATION_DURATION);
                        isSaveButtonVisible = false;
                    } else {
                        btnSaveAllOutput.setVisibility(View.GONE);
                        isSaveButtonVisible = false;
                    }
            }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {

                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                        if (checkVolumeFilter(BLOCKING_VOLUME_UP_KEY)) {                           
                                return true; 
                            } else {
                                return super.onKeyDown(keyCode, event); 
                            }

                    } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        if (checkVolumeFilter(BLOCKING_VOLUME_DOWN_KEY)) {
                                return true; 
                            } else {
                                return super.onKeyDown(keyCode, event); 
                            }
                    }
                return super.onKeyDown(keyCode, event);
            }

        public boolean checkVolumeFilter(String key) {
                String prefKey = KEY_VOLUME_PREFIX + key.toUpperCase();
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getBoolean(prefKey, true);
            }

        public boolean isDeleteFilterEnabled(String typeName) {
                final String key = KEY_DELETE_PREFIX + typeName;
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getBoolean(key, false); 
            }

        public boolean isTypeEnabled(TerminalOutputType type) {
                final String key = KEY_OUTPUT_PREFIX + type.name();
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getBoolean(key, true); 
            }

        public boolean isTypeEnabledForLogSave(TerminalOutputType type) {
                final String key = KEY_LOG_SAVE_PREFIX + type.name();
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                return prefs.getBoolean(key, true); 
            }

        private boolean checkStoragePermission() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                                ActivityCompat.requestPermissions(this,
                                                                  new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                  PERMISSION_REQUEST_CODE);
                                return false;
                            }
                    }
                return true;
            }

        @Override
        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (requestCode == PERMISSION_REQUEST_CODE) {
                        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                saveLogToFile();
                            } else {
                                Toast.makeText(this, "Izin penyimpanan ditolak. Tidak dapat menyimpan log.", Toast.LENGTH_LONG).show();
                            }
                    }
            }

        private String getAllLogContent() {
                StringBuilder sb = new StringBuilder();
                synchronized (outputDataList) {
                        for (final TerminalLine line : outputDataList) { 
                                if (isTypeEnabledForLogSave(line.type)) {
                                        sb.append(line.text).append("\n"); 
                                    }
                            }
                    }
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '\n') {
                        sb.setLength(sb.length() - 1);
                    }
                return sb.toString();
            }

        private void saveLogToFile() {
                final String logContent = getAllLogContent();
                if (TextUtils.isEmpty(logContent)) {
                        Toast.makeText(this, "Tidak ada output untuk disimpan.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                final File baseDir = getExternalFilesDir(null); 
                if (baseDir == null) {
                        Toast.makeText(this, "Gagal mengakses direktori penyimpanan.", Toast.LENGTH_LONG).show();
                        return;
                    }

                final File logDir = new File(baseDir, LOG_DIRECTORY);
                if (!logDir.exists()) {
                        logDir.mkdirs();
                    }

                final String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault()).format(new Date());
                final String fileName = "AmberShell_" + timestamp + ".log";

                final File logFile = new File(logDir, fileName);

                FileOutputStream fos = null;
                try {
                        fos = new FileOutputStream(logFile);
                        fos.write(logContent.getBytes());
                        final String displayMessage = "Log saved to " + logDir.getAbsolutePath() + "/\n" + fileName;

                        runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                            LayoutInflater inflater = getLayoutInflater();
                                            View layout = inflater.inflate(R.layout.custom_save_notification, 
                                                                           (ViewGroup) findViewById(R.id.custom_toast_container));

                                            TextView text = layout.findViewById(R.id.text_log_path);
                                            text.setText(displayMessage);

                                            Toast toast = new Toast(getApplicationContext());
                                            int yOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
                                            toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, yOffset); 

                                            toast.setDuration(Toast.LENGTH_LONG);
                                            toast.setView(layout);
                                            toast.show();
                                        }
                                });

                    } catch (final IOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                            Toast.makeText(QuickTerminalActivity.this, "Gagal menyimpan log: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                });
                    } finally {
                        if (fos != null) {
                                try {
                                        fos.close();
                                    } catch (IOException ignored) {}
                            }
                    }
            }

        private void stopCurrentProcess() {
                if (currentProcess != null) {
                        try {
                                currentProcess.destroy(); 
                                appendOutput(CANCELLED_TAG, TerminalOutputType.TYPE_THROW, true);
                            } catch (Exception e) {
                                Toast.makeText(this, "Gagal menghentikan proses: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            } finally {
                                currentProcess = null;
                                resetButtonState();
                            }
                    } else {
                        Toast.makeText(this, "Tidak ada proses yang berjalan.", Toast.LENGTH_SHORT).show();
                    }
            }

        private void resetButtonState() {
                btnRunCommand.setEnabled(true);
                if (btnStopLoping != null) {
                        btnStopLoping.setEnabled(false); 
                        btnStopLoping.setVisibility(View.VISIBLE); 
                        btnStopLoping.setColorFilter(FROZEN_COLOR, PorterDuff.Mode.SRC_IN);
                    }
                inputCommand.requestFocus();
            }

        private void clearOutput() {
                clearOutput(true);
            }

        private void clearOutput(final boolean clearInput) {
                outputDataList.clear();
                checkSaveButtonDataVisibility(); 

                if (adapter != null) {
                        adapter.rebuildFilter();
                    } else {
                        adapter.notifyDataSetChanged();
                    }

                if (clearInput) {
                        inputCommand.setText("");
                    }
            }

        private void clearOutputFiltered() {
                List<TerminalLine> retainedLines = new ArrayList<TerminalLine>();
                synchronized (outputDataList) {
                        for (final TerminalLine line : outputDataList) {
                                if (line.type == TerminalOutputType.TYPE_STDOUT) {
                                        retainedLines.add(line);
                                    }
                            }
                        outputDataList.clear();
                        outputDataList.addAll(retainedLines);
                    }
                checkSaveButtonDataVisibility(); 

                if (adapter != null) {
                        adapter.rebuildFilter();
                    } else {
                        adapter.notifyDataSetChanged();
                    }
            }


        private void scheduleScroll() {
                if (isScrollScheduled) return;

                isScrollScheduled = true;
                uiHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                    layoutManager.scrollToPosition(adapter.getItemCount() - 1);
                                    isScrollScheduled = false;
                                }
                        }, SCROLL_DELAY_MS);
            }

        private void appendOutput(final String text, final TerminalOutputType type, final boolean doScroll) {
                String finalLine;
                if (type == TerminalOutputType.TYPE_SPACE) {
                        finalLine = "";
                    } else if (doScroll) {
                        finalLine = text.trim();
                    } else {
                        finalLine = text;
                    }

                if (doScroll && TextUtils.isEmpty(finalLine) && type != TerminalOutputType.TYPE_SPACE) {
                        if (doScroll) {
                                scheduleScroll();
                            }
                        return;
                    }

                synchronized (outputDataList) {
                        outputDataList.add(new TerminalLine(finalLine, type)); 
                    }
                checkSaveButtonDataVisibility(); 

                if (doScroll) {
                        scheduleScroll();
                    }

                if (type == TerminalOutputType.TYPE_START || type == TerminalOutputType.TYPE_EXIT || type == TerminalOutputType.TYPE_THROW || type == TerminalOutputType.TYPE_COMMAND) {
                        if (adapter != null) {
                                adapter.rebuildFilter();
                            }
                    }
            }


        private void runProcessStreams(final Process process, final String pid) throws InterruptedException {
                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                final boolean[] hasOutput = {false}; 
                final StreamReader outReader = new StreamReader(stdout, false, hasOutput, uiHandler);
                final StreamReader errReader = new StreamReader(stderr, true, hasOutput, uiHandler);

                final Thread outThread = new Thread(outReader);
                final Thread errThread = new Thread(errReader);

                outThread.start();
                errThread.start();

                final int exitCode = process.waitFor(); 

                outThread.join();
                errThread.join();

                outReader.forceFinalUpdate();
                errReader.forceFinalUpdate();

                runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                    if (currentProcess != null) {
                                            final String doneMsg = EXIT_TAG + exitCode; 
                                            appendOutput(doneMsg, TerminalOutputType.TYPE_EXIT, true);
                                        }
                                    currentProcess = null;
                                    resetButtonState();
                                }
                        });
            }

        private class TerminalOutputAdapter extends RecyclerView.Adapter<TerminalOutputAdapter.LineViewHolder> {
                private final List<TerminalLine> lines; 
                private final QuickTerminalActivity activity; 
                private final List<TerminalLine> filteredLines; 
                private final int defaultTextColor;
                private final int ERROR_COLOR; 

                public TerminalOutputAdapter(List<TerminalLine> lines, QuickTerminalActivity activity) {
                        this.lines = lines;
                        this.activity = activity;
                        this.filteredLines = new ArrayList<TerminalLine>();
                        this.defaultTextColor = ContextCompat.getColor(activity, R.color.colorOnPrimary);
                        this.ERROR_COLOR = activity.ERROR_COLOR;
                        setHasStableIds(false);
                        rebuildFilter(); 
                    }

                public void rebuildFilter() {
                        filteredLines.clear();
                        for (final TerminalLine line : lines) {
                                if (line == null) { 
                                        continue; 
                                    }
                                if (activity.isTypeEnabled(line.type)) { 
                                        filteredLines.add(line);
                                    }
                            }
                        notifyDataSetChanged();
                        activity.scheduleScroll(); 
                    }

                @Override
                public LineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_terminal_line, parent, false);
                        return new LineViewHolder(view);
                    }

                @Override
                public void onBindViewHolder(LineViewHolder holder, int position) {
                        final TerminalLine lineObj = filteredLines.get(position);

                        holder.textLine.setText(lineObj.text);

                        holder.textLine.setTypeface(Typeface.MONOSPACE);
                        if (lineObj.type == TerminalOutputType.TYPE_STDERR || 
                            lineObj.type == TerminalOutputType.TYPE_THROW) {
                                holder.textLine.setTextColor(ERROR_COLOR); 
                            } else {
                                holder.textLine.setTextColor(defaultTextColor);
                            }
                    }

                @Override
                public int getItemCount() {
                        return filteredLines.size(); 
                    }

                public class LineViewHolder extends RecyclerView.ViewHolder {
                        public TextView textLine;

                        public LineViewHolder(View itemView) {
                                super(itemView);
                                textLine = itemView.findViewById(R.id.text_line);
                                textLine.setHorizontallyScrolling(true);
                                textLine.setSingleLine(false); 
                            }
                    }
            }

        private class StreamReader implements Runnable {
                private final BufferedReader reader;
                private final boolean isError;
                private final boolean[] hasOutputFlag;
                private final Handler handler;
                private final List<TerminalLine> batchLineBuffer = new ArrayList<TerminalLine>();
                private boolean isScheduled = false;

                private final Runnable myUiUpdateRunnable = new Runnable() {
                        @Override
                        public void run() {
                                if (batchLineBuffer.isEmpty()) {
                                        isScheduled = false;
                                        try {
                                                pendingUpdateRunnables.remove(this); 
                                            } catch (Exception ignored) {}
                                        return;
                                    }

                                synchronized (outputDataList) {
                                        outputDataList.addAll(batchLineBuffer);
                                        batchLineBuffer.clear();
                                    }

                                if (adapter != null) {
                                        adapter.rebuildFilter();
                                    }
                                checkSaveButtonDataVisibility(); 

                                isScheduled = false;
                                try {
                                        pendingUpdateRunnables.remove(this); 
                                    } catch (Exception ignored) {}
                            }
                    };

                StreamReader(BufferedReader reader, boolean isError, boolean[] hasOutputFlag, Handler uiHandler) {
                        this.reader = reader;
                        this.isError = isError;
                        this.hasOutputFlag = hasOutputFlag;
                        this.handler = uiHandler;
                    }

                public void forceFinalUpdate() {
                        handler.removeCallbacks(myUiUpdateRunnable);

                        if (!batchLineBuffer.isEmpty()) {
                                handler.post(myUiUpdateRunnable);
                            } else if (isScheduled) {
                                try {
                                        pendingUpdateRunnables.remove(myUiUpdateRunnable); 
                                    } catch (Exception ignored) {}
                                isScheduled = false;
                            }
                    }

                @Override
                public void run() {
                        try {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                        hasOutputFlag[0] = true;
                                        final TerminalOutputType type = isError ? TerminalOutputType.TYPE_STDERR : TerminalOutputType.TYPE_STDOUT; 

                                        synchronized (batchLineBuffer) {
                                                final String finalLine = line; 
                                                batchLineBuffer.add(new TerminalLine(finalLine, type)); 
                                            }

                                        if (!isScheduled) {
                                                isScheduled = true;
                                                pendingUpdateRunnables.add(myUiUpdateRunnable); 
                                                handler.postDelayed(myUiUpdateRunnable, UI_UPDATE_INTERVAL_MS);
                                            }
                                    }
                            } catch (IOException ignored) {
                            }
                    }
            }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (currentProcess != null) {
                        currentProcess.destroy();
                    }
                if (executor != null) {
                        try {
                                executor.shutdownNow();
                            } catch (Throwable ignored) {}
                    }
                if (uiHandler != null) {
                        uiHandler.removeCallbacksAndMessages(null); 
                    }
                pendingUpdateRunnables.clear();
                if (showAnimator != null) showAnimator.cancel();
                if (hideAnimator != null) hideAnimator.cancel();
            }

        private static String[] splitArgs(String cmd) {
                if (cmd == null) return new String[0];
                ArrayList<String> parts = new ArrayList<String>();
                StringBuilder cur = new StringBuilder();
                boolean inQuote = false;
                char quoteChar = 0;
                for (int i = 0; i < cmd.length(); i++) {
                        char c = cmd.charAt(i);
                        if (!inQuote) {
                                if (c == '\'' || c == '"') {
                                        inQuote = true;
                                        quoteChar = c;
                                    } else if (Character.isWhitespace(c)) {
                                        if (cur.length() > 0) {
                                                parts.add(cur.toString());
                                                cur.setLength(0);
                                            }
                                    } else {
                                        cur.append(c);
                                    }
                            } else {
                                if (c == quoteChar) inQuote = false;
                                else cur.append(c);
                            }
                    }
                if (cur.length() > 0) parts.add(cur.toString());
                return parts.toArray(new String[parts.size()]);
            }
    }


