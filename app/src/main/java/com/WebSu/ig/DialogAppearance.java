package com.WebSu.ig;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences; // Import SharedPreferences
import android.content.res.ColorStateList;
import android.graphics.drawable.*;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class DialogAppearance {

    // Konstanta untuk perintah shell
    private static final String CMD_NIGHT_YES = "cmd uimode night yes";
    private static final String CMD_NIGHT_NO = "cmd uimode night no";
    private static final String CMD_NIGHT_AUTO = "cmd uimode night auto";
    private static final String THEME_CHOICE_KEY = "theme_choice"; 
    private static final String TAG = "DialogAppearance"; 

    // Nilai untuk SharedPreferences
    private static final String CHOICE_OTOMATIS = "Otomatis";
    private static final String CHOICE_TERANG = "Terang";
    private static final String CHOICE_GELAP = "Gelap";
    // Nilai default adalah Terang
    private static final String DEFAULT_CHOICE = CHOICE_TERANG; 

    // === 1. FUNGSI UNTUK MENGELOLA SHARED PREFERENCES ===
    private static void saveThemeChoice(Context ctx, String choice) {
        SharedPreferences prefs = ctx.getSharedPreferences("DialogPrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(THEME_CHOICE_KEY, choice).apply();
    }

    private static String getThemeChoice(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("DialogPrefs", Context.MODE_PRIVATE);
        return prefs.getString(THEME_CHOICE_KEY, DEFAULT_CHOICE);
    }

    // === 2. FUNGSI UTAMA SHOW() ===
    public static void show(final Context ctx) {
        final Dialog dialog = new Dialog(ctx);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        final float d = ctx.getResources().getDisplayMetrics().density;

        // Ambil pilihan tema yang tersimpan
        final String savedChoice = getThemeChoice(ctx);

        // ... [BAGIAN UI: ROOT, DRAG BAR, TITLE] ...

        // === ROOT CONTENT ===
        final LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        int rootPadding = (int) (20 * d);
        root.setPadding(rootPadding, (int) (16 * d), rootPadding, (int) (20 * d));

        GradientDrawable rootBg = new GradientDrawable();
        // Menggunakan R.color.background_primary
        rootBg.setColor(ContextCompat.getColor(ctx, R.color.colorBackground));
        rootBg.setCornerRadii(new float[]{
								  40 * d, 40 * d, 40 * d, 40 * d,
								  0, 0, 0, 0
							  });
        root.setAlpha(0.98f);
        root.setBackground(rootBg);

        // === DRAG BAR ===
        final View dragBar = new View(ctx);
        LinearLayout.LayoutParams dragParams = new LinearLayout.LayoutParams((int) (60 * d), (int) (6 * d));
        dragParams.gravity = Gravity.CENTER_HORIZONTAL;
        dragParams.topMargin = (int) (8 * d);
        dragParams.bottomMargin = (int) (18 * d);
        dragBar.setLayoutParams(dragParams);

        GradientDrawable dragShape = new GradientDrawable();
        dragShape.setCornerRadius(3 * d);
        dragShape.setColor(ContextCompat.getColor(ctx, R.color.colorOnPrimary));
        dragBar.setBackground(dragShape);
        dragBar.setAlpha(0.3f);
        root.addView(dragBar);

        // === TITLE ===
        final TextView title = new TextView(ctx);
        title.setText("Appearance");
        title.setTextColor(ContextCompat.getColor(ctx, R.color.colorOnPrimary));
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        title.setPadding(0, 0, 0, (int) (20 * d));
        root.addView(title);

        // === CARD 1: Auto Theme ===
        final LinearLayout card = makeRoundedCard(ctx, d);
        root.addView(card);

        final LinearLayout autoRow = new LinearLayout(ctx);
        autoRow.setOrientation(LinearLayout.HORIZONTAL);
        autoRow.setGravity(Gravity.CENTER_VERTICAL);

        final TextView autoText = new TextView(ctx);
        autoText.setText("Auto Theme");
        autoText.setTextSize(16);
        // Menggunakan R.color.text_primary
        autoText.setTextColor(ContextCompat.getColor(ctx, R.color.colorOnPrimary));

        final logd swAuto = new logd(ctx);

        LinearLayout.LayoutParams autoTextParams =
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        autoTextParams.rightMargin = (int) (12 * d);

        LinearLayout.LayoutParams switchParams =
            new LinearLayout.LayoutParams((int) (55 * d), (int) (35 * d));

        autoRow.addView(autoText, autoTextParams);
        autoRow.addView(swAuto, switchParams);
        card.addView(autoRow);

        final TextView descAuto = new TextView(ctx);
        descAuto.setText("Select theme automatically");
        // Menggunakan R.color.text_secondary
        descAuto.setTextColor(ContextCompat.getColor(ctx, R.color.colorSurface));
        descAuto.setTextSize(13);
        descAuto.setPadding(0, (int) (8 * d), 0, (int) (20 * d));
        card.addView(descAuto);

        // === RADIO BUTTONS (SEGMENTED CONTROL) ===
        final RadioGroup rg = createSegmentedRadioGroup(ctx, d);

        // Ambil RadioButton dari RadioGroup
        final RadioButton rbOtomatis = (RadioButton) rg.getChildAt(0);
        final RadioButton rbTerang = (RadioButton) rg.getChildAt(1);
        final RadioButton rbGelap = (RadioButton) rg.getChildAt(2);

        // Beri ID yang unik dan tetapkan teks
        final int idOtomatis = View.generateViewId();
        final int idTerang = View.generateViewId();
        final int idGelap = View.generateViewId();

        rbOtomatis.setText(CHOICE_OTOMATIS);
        rbTerang.setText(CHOICE_TERANG);
        rbGelap.setText(CHOICE_GELAP);

        rbOtomatis.setId(idOtomatis);
        rbTerang.setId(idTerang);
        rbGelap.setId(idGelap);

        // Terapkan pilihan yang tersimpan ke UI saat dialog dibuat
        if (savedChoice.equals(CHOICE_OTOMATIS)) {
            swAuto.setChecked(true);
            rbOtomatis.setChecked(true);
        } else if (savedChoice.equals(CHOICE_GELAP)) {
            swAuto.setChecked(false);
            rbGelap.setChecked(true);
        } else { // DEFAULT: CHOICE_TERANG
            swAuto.setChecked(false);
            rbTerang.setChecked(true);
        }

        for (int i = 0; i < rg.getChildCount(); i++) {
            rg.getChildAt(i).setEnabled(true);
        }

        card.addView(rg);

        // === LISTENER UNTUK RADIOGROUP ===
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
				private String currentChoice = getThemeChoice(ctx);

				@Override
				public void onCheckedChanged(RadioGroup group, int checkedId) {
					String command = null;
					String newChoice = null;

					if (checkedId == idOtomatis) {
						command = CMD_NIGHT_AUTO;
						newChoice = CHOICE_OTOMATIS;
						swAuto.setChecked(true);
					} else if (checkedId == idTerang) {
						command = CMD_NIGHT_NO;
						newChoice = CHOICE_TERANG;
						swAuto.setChecked(false);
					} else if (checkedId == idGelap) {
						command = CMD_NIGHT_YES;
						newChoice = CHOICE_GELAP;
						swAuto.setChecked(false);
					}

					if (newChoice != null && !newChoice.equals(currentChoice)) {
						// Hanya eksekusi dan simpan jika pilihan BERUBAH
						executeShellCommand(ctx, command);
						saveThemeChoice(ctx, newChoice);
						currentChoice = newChoice; // Update currentChoice
					}
				}
			});

        // === LISTENER UNTUK SWITCH 'Auto Theme' ===
        swAuto.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					// Gunakan post() untuk menunda sedikit eksekusi sampai RadioGroup
					// selesai memproses perubahan state yang disebabkan oleh swAuto.
					// Namun, kita bisa langsung memanggil rg.check(), dan listener rg akan menangani eksekusi.

					// Pastikan untuk TIDAK menggunakan swAuto.isChecked() di dalam onClick
					// karena state-nya mungkin belum diupdate. Gunakan isChecked() pada View v.
					if (((logd)v).isChecked()) {
						// Jika Auto Theme diaktifkan, pilih 'Otomatis' di RadioGroup
						rg.check(idOtomatis);
					} else {
						// Jika Auto Theme dinonaktifkan, default ke 'Terang'
						rg.check(idTerang);
					}
				}
			});


        final LinearLayout dynamicCard = makeRoundedCard(ctx, d);
        final LinearLayout dynamicRow = new LinearLayout(ctx);
        dynamicRow.setOrientation(LinearLayout.HORIZONTAL);
        dynamicRow.setGravity(Gravity.CENTER_VERTICAL);

        final TextView dynText = new TextView(ctx);
        dynText.setText("Dynamic Color");
        dynText.setTextSize(16);
        dynText.setTextColor(ContextCompat.getColor(ctx, R.color.colorOnPrimary));

        final logd swDynamic = new logd(ctx);
        // Di gambar: Switch Dynamic Color dicek
        swDynamic.setChecked(false);

        LinearLayout.LayoutParams dynTextParams =
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        dynTextParams.rightMargin = (int) (12 * d);

        LinearLayout.LayoutParams dynSwitchParams =
            new LinearLayout.LayoutParams((int) (55 * d), (int) (35 * d));

        dynamicRow.addView(dynText, dynTextParams);
        dynamicRow.addView(swDynamic, dynSwitchParams);
        dynamicCard.addView(dynamicRow);
        root.addView(dynamicCard);

        // === ScrollView & Wrapper ===
        final ScrollView scrollView = new ScrollView(ctx);
        scrollView.setFillViewport(true);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.addView(root);

        final FrameLayout wrapper = new FrameLayout(ctx);
        wrapper.setBackgroundColor(0x00000000);
        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        wrapper.setLayoutParams(wrapParams);

        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM);
        wrapper.addView(scrollView, bottomParams);

        dialog.setContentView(wrapper);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(0x00000000));
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
            window.getAttributes().windowAnimations = android.R.style.Animation_InputMethod;
        }

        final ScrollView panel = scrollView;
        final Dialog dlg = dialog;
        final int screenHeight = ctx.getResources().getDisplayMetrics().heightPixels;

        // === Swipe to dismiss (Biarkan fungsi UI ini tetap ada) ===
        panel.setOnTouchListener(new View.OnTouchListener() {
				float startY;
				float startPanelY;
				boolean dragging = false;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							startY = event.getRawY();
							startPanelY = panel.getY();
							dragging = false;
							break;

						case MotionEvent.ACTION_MOVE:
							float dy = event.getRawY() - startY;
							if (panel.getScrollY() == 0 && dy > 0) {
								float newY = startPanelY + dy;
								if (newY > startPanelY) {
									panel.setY(newY);
									dragging = true;
									return true;
								}
							}
							break;

						case MotionEvent.ACTION_UP:
							if (dragging) {
								float finalY = panel.getY();
								float threshold = screenHeight * 0.25f;

								if (finalY > threshold) {
									panel.animate()
										.translationY(screenHeight)
										.setDuration(250)
										.withEndAction(new Runnable() {
											@Override
											public void run() {
												dlg.dismiss();
											}
										})
										.start();
								} else {
									panel.animate()
										.translationY(0)
										.setDuration(200)
										.start();
								}
								return true;
							}
							break;
					}
					return false;
				}
			});


        dialog.show();

        if (savedChoice.equals(CHOICE_TERANG)) {
			executeShellCommand(ctx, CMD_NIGHT_NO);
        } else if (savedChoice.equals(CHOICE_GELAP)) {
			executeShellCommand(ctx, CMD_NIGHT_YES);
        } else if (savedChoice.equals(CHOICE_OTOMATIS)) {
			executeShellCommand(ctx, CMD_NIGHT_AUTO);
        }
    }

    // === Fungsi untuk mengeksekusi perintah shell ===
    private static void executeShellCommand(final Context ctx, final String command) {
        new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, "Executing command: " + command);
					ShellExecutor.Result result = ShellExecutor.execSync(command, ctx);

					if (result.exitCode == 0) {
						Log.i(TAG, "Command executed successfully: " + command);
					} else {
						Log.e(TAG, "Command failed: " + command + " | Exit code: " + result.exitCode + " | Error: " + result.stderr);
					}
				}
			}).start();
    }


    // === Helper: Rounded Floating Card + Ripple Effect (TETAP TANPA BORDER) ===
    private static LinearLayout makeRoundedCard(Context ctx, float d) {
        GradientDrawable cardBg = new GradientDrawable();
        // Menggunakan R.color.card_background
        cardBg.setColor(ContextCompat.getColor(ctx, R.color.colorBgd));
        cardBg.setCornerRadius(30 * d);
        // Menghilangkan border:
		cardBg.setStroke(0, 0);


        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg);
        int padding = (int) (20 * d);
        card.setPadding(padding, padding, padding, padding);
        card.setElevation(6 * d);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (12 * d);
        cardParams.setMargins(margin, margin, margin, margin);
        card.setLayoutParams(cardParams);

        addTouchEffect(card);
        return card;
    }

    private static void addTouchEffect(View v) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorDrawable mask = new ColorDrawable(ContextCompat.getColor(v.getContext(), R.color.colorPrimary));
            RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(ContextCompat.getColor(v.getContext(), R.color.colorPrimary)),
                v.getBackground(),
                mask
            );
            v.setBackground(ripple);
        }
    }

    private static float[] getSegmentRadii(float d, int index, int total) {
        float innerRadius = 28 * d;
        float[] radii = new float[8];
        if (index == 0) {
			radii = new float[]{innerRadius, innerRadius, 0, 0, 0, 0, innerRadius, innerRadius};
        } else if (index == total - 1) {
            radii = new float[]{0, 0, innerRadius, innerRadius, innerRadius, innerRadius, 0, 0};
        } else {
            radii = new float[]{0, 0, 0, 0, 0, 0, 0, 0};
        }
        return radii;
    }

    private static RadioGroup createSegmentedRadioGroup(Context ctx, float d) {
        final RadioGroup rg = new RadioGroup(ctx);
        rg.setOrientation(LinearLayout.HORIZONTAL);

        final int STROKE_COLOR = ContextCompat.getColor(ctx, R.color.colorSurface);
        final int STROKE_WIDTH = (int) (1 * d);
        float radius = 30 * d;

        GradientDrawable containerBg = new GradientDrawable();
        containerBg.setCornerRadius(radius);
        containerBg.setStroke(STROKE_WIDTH, STROKE_COLOR);
        containerBg.setColor(STROKE_COLOR);
        rg.setBackground(containerBg);

        LinearLayout.LayoutParams rgParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, (int) (40 * d));
        rgParams.setMargins(0, 0, 0, (int) (10 * d));
        rg.setLayoutParams(rgParams);
        rg.setPadding(STROKE_WIDTH, STROKE_WIDTH, STROKE_WIDTH, STROKE_WIDTH);

        for (int i = 0; i < 3; i++) {
            RadioButton rb = new RadioButton(ctx);
            rb.setText("");
            rb.setGravity(Gravity.CENTER);
            rb.setButtonDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

            ColorStateList textColors = new ColorStateList(
                new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
                },
                new int[]{
                    ContextCompat.getColor(ctx, R.color.colorBackground),
                    ContextCompat.getColor(ctx, R.color.colorOnPrimary)
                }
            );
            rb.setTextColor(textColors);
            rb.setTextSize(16);
            int padX = (int) (1 * d);
            rb.setPadding(padX, 0, padX, 0);
            rb.setBackground(makeSegmentedRadioButtonDrawable(ctx, d, i, 3));

            LinearLayout.LayoutParams rbParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) {
				rbParams.leftMargin = STROKE_WIDTH;
            }
            rb.setLayoutParams(rbParams);

            rg.addView(rb);
        }

        return rg;
    }

    private static Drawable makeSegmentedRadioButtonDrawable(Context ctx, float d, int index, int total) {

        final int SEGMENT_COLOR = ContextCompat.getColor(ctx, R.color.blockSur);
        final int UNCHECKED_COLOR = ContextCompat.getColor(ctx, R.color.colorBgd);

        float[] radii = getSegmentRadii(d, index, total);

        GradientDrawable checkedBg = new GradientDrawable();
        checkedBg.setColor(SEGMENT_COLOR);
        checkedBg.setStroke(0, SEGMENT_COLOR);
        checkedBg.setCornerRadii(radii);

        GradientDrawable uncheckedBg = new GradientDrawable();
        uncheckedBg.setColor(UNCHECKED_COLOR);
        uncheckedBg.setCornerRadii(radii);
        uncheckedBg.setStroke(0, UNCHECKED_COLOR);

        StateListDrawable stateListDrawable = new StateListDrawable();
        stateListDrawable.addState(new int[]{android.R.attr.state_checked}, checkedBg);
        stateListDrawable.addState(new int[]{}, uncheckedBg);

        return stateListDrawable;
    }
}

