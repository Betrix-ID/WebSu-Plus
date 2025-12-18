package com.WebSu.ig;

import android.content.Context;
import android.graphics.Color;

public class ThemeSelection {

    // ========================
    // Warna Dasar (Disesuaikan untuk visibilitas yang lebih baik)
    // ========================
    public static final int AMOLED_BLACK = Color.parseColor("#000000"); // Hitam murni
    public static final int PRIMARY = Color.parseColor("#673AB7"); // Ungu Khas
    public static final int PRIMARY_DARK = Color.parseColor("#512DA8"); // Ungu Gelap
    public static final int SECONDARY = Color.parseColor("#00BCD4"); // Cyan Khas
    public static final int SECONDARY_DARK = Color.parseColor("#0097A7"); // Cyan Gelap
    public static final int TERTIARY = Color.parseColor("#FFC107"); // Amber Khas
    public static final int RED = Color.parseColor("#F44336"); // Merah Error
    public static final int DARK_BLEND = Color.parseColor("#1C1B1F"); // Warna dasar untuk Surface di Dark Theme
    public static final int WHITE = Color.WHITE; // Putih murni

    // ========================
    // Fungsi Blending (Tetap)
    // ========================
    public static int blend(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        int a = (int) ((Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio));
        int r = (int) ((Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio));
        int g = (int) ((Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio));
        int b = (int) ((Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio));
        return Color.argb(a, r, g, b);
    }

    // ========================
    // Tema Warna (Tetap)
    // ========================
    public static class ColorScheme {
        public int primary;
		public int onPrimary;
		public int primaryContainer;
		public int onPrimaryContainer;

		public int secondary;
		public int onSecondary;
		public int secondaryContainer;
		public int onSecondaryContainer;

		public int tertiary;
		public int onTertiary;
		public int tertiaryContainer;
		public int onTertiaryContainer;

		public int surface;
		public int onSurface;
		public int surfaceVariant;
		public int onSurfaceVariant;
		public int surfaceContainer;
		public int surfaceContainerLow;
		public int surfaceContainerLowest;
		public int surfaceContainerHigh;
		public int surfaceContainerHighest;
		public int errorContainer;
		public int outline;
		public int background;
    }

    // ========================
    // Dark Theme (Disesuaikan)
    // ========================
    public static ColorScheme getDarkColorScheme() {
        ColorScheme scheme = new ColorScheme();

        // Palet Utama
        scheme.primary = PRIMARY; 
        scheme.onPrimary = WHITE; // Teks di atas Primary harus putih
        scheme.primaryContainer = PRIMARY_DARK; 
        scheme.onPrimaryContainer = WHITE;

        // Palet Sekunder
        scheme.secondary = SECONDARY;
        scheme.onSecondary = AMOLED_BLACK; // Teks di atas Secondary harus gelap
        scheme.secondaryContainer = blend(SECONDARY_DARK, AMOLED_BLACK, 0.4f);
        scheme.onSecondaryContainer = WHITE;

        // Palet Tersier
        scheme.tertiary = TERTIARY;
        scheme.onTertiary = AMOLED_BLACK;
        scheme.tertiaryContainer = blend(TERTIARY, AMOLED_BLACK, 0.4f);
        scheme.onTertiaryContainer = WHITE;

        // Surface dan Background (Penting untuk kontras di Dark Theme)
        scheme.surfaceContainerLowest = AMOLED_BLACK; // Paling gelap (AMOLED)
        scheme.background = scheme.surfaceContainerLowest;
        scheme.surfaceContainerLow = blend(DARK_BLEND, AMOLED_BLACK, 0.8f);
        scheme.surfaceContainer = DARK_BLEND; // Normal
        scheme.surface = scheme.surfaceContainer;
        scheme.surfaceContainerHigh = blend(DARK_BLEND, WHITE, 0.1f);
        scheme.surfaceContainerHighest = blend(DARK_BLEND, WHITE, 0.2f);

        scheme.onSurface = WHITE; // Teks utama harus putih
        scheme.surfaceVariant = blend(DARK_BLEND, WHITE, 0.3f);
        scheme.onSurfaceVariant = blend(WHITE, AMOLED_BLACK, 0.2f); // Teks/ikon pendukung

        scheme.errorContainer = blend(RED, AMOLED_BLACK, 0.5f);
        scheme.outline = blend(DARK_BLEND, WHITE, 0.3f);

        return scheme;
    }

    // ========================
    // Light Theme (Disesuaikan)
    // ========================
    public static ColorScheme getLightColorScheme() {
        ColorScheme scheme = new ColorScheme();

        // Palet Utama
        scheme.primary = PRIMARY_DARK; // Lebih gelap agar terlihat kontras di latar belakang terang
        scheme.onPrimary = WHITE;
        scheme.primaryContainer = blend(PRIMARY, WHITE, 0.6f);
        scheme.onPrimaryContainer = AMOLED_BLACK;

        // Palet Sekunder
        scheme.secondary = SECONDARY_DARK;
        scheme.onSecondary = WHITE;
        scheme.secondaryContainer = blend(SECONDARY, WHITE, 0.8f);
        scheme.onSecondaryContainer = AMOLED_BLACK;

        // Palet Tersier (Dipindahkan ke atas untuk konsistensi)
        scheme.tertiary = TERTIARY;
        scheme.onTertiary = AMOLED_BLACK;
        scheme.tertiaryContainer = blend(TERTIARY, WHITE, 0.4f);
        scheme.onTertiaryContainer = AMOLED_BLACK;

        // Surface dan Background (Penting untuk kontras di Light Theme)
        scheme.surfaceContainerLowest = WHITE; // Paling terang
        scheme.background = scheme.surfaceContainerLowest;
        scheme.surfaceContainerLow = blend(DARK_BLEND, WHITE, 0.95f);
        scheme.surfaceContainer = blend(DARK_BLEND, WHITE, 0.9f); // Normal
        scheme.surface = scheme.surfaceContainer;
        scheme.surfaceContainerHigh = blend(DARK_BLEND, WHITE, 0.85f);
        scheme.surfaceContainerHighest = blend(DARK_BLEND, WHITE, 0.8f);

        scheme.onSurface = AMOLED_BLACK; // Teks utama harus hitam
        scheme.surfaceVariant = blend(DARK_BLEND, WHITE, 0.7f);
        scheme.onSurfaceVariant = blend(DARK_BLEND, AMOLED_BLACK, 0.2f);

        scheme.errorContainer = blend(RED, WHITE, 0.7f);
        scheme.outline = blend(DARK_BLEND, WHITE, 0.4f);

        return scheme;
    }

    // ===================================
    // Dynamic Theme (Placeholder untuk Material You/Monet)
    // ===================================
    /**
     * Catatan: Untuk tema dinamis, Anda perlu mengambil warna dari Android API (misalnya,
     * menggunakan ThemeUtils/DynamicColors di pustaka Material Components)
     * Warna di sini HANYA menggunakan tema statis sebagai FALLBACK.
     */
    public static ColorScheme getDynamicColorScheme(Context context, boolean isDarkTheme) {
        // Logika untuk mengambil warna dinamis (seperti dari wallpaper)
        // int dynamicPrimary = DynamicColorUtils.getPrimaryColor(context);

        // --- FALLBACK ke skema statis ---
        if (isDarkTheme) {
            return getDarkColorScheme(); 
        } else {
            return getLightColorScheme();
        }
    }


    // ===========================================
    // Pilih tema sesuai preferensi (Diperbarui)
    // ===========================================
    /**
     * Memilih ColorScheme berdasarkan preferensi tema dan Dynamic Color.
     * @param context Context untuk mengambil warna dinamis (jika diaktifkan).
     * @param darkTheme True jika mode gelap diaktifkan.
     * @param dynamicColorEnabled True jika tema dinamis (Material You) diaktifkan.
     */
    public static ColorScheme getColorScheme(Context context, boolean darkTheme, boolean dynamicColorEnabled) {
        if (dynamicColorEnabled) {
            return getDynamicColorScheme(context, darkTheme);
        } else if (darkTheme) {
            return getDarkColorScheme();
        } else {
            return getLightColorScheme();
        }
    }
}

