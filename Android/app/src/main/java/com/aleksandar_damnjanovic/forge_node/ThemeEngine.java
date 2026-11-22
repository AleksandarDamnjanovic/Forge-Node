package com.aleksandar_damnjanovic.forge_node;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors UI/forge-node/src/main/resources/static/theme.js palettes and UI styles
 * so native Android bars, buttons, and Login screen match the selected Web App theme.
 */
public class ThemeEngine {

    public static class Palette {
        public final int bgApp;
        public final int bgPanel;
        public final int bgCard;
        public final int bgInput;
        public final int bgHover;
        public final int border;
        public final int borderFocus;
        public final int textMain;
        public final int textMuted;
        public final int primary;
        public final int success;
        public final int warning;
        public final int danger;
        public final int purple;
        public final int cyan;

        public Palette(String bgApp, String bgPanel, String bgCard, String bgInput, String bgHover,
                       String border, String borderFocus, String textMain, String textMuted,
                       String primary, String success, String warning, String danger,
                       String purple, String cyan) {
            this.bgApp = Color.parseColor(bgApp);
            this.bgPanel = Color.parseColor(bgPanel);
            this.bgCard = Color.parseColor(bgCard);
            this.bgInput = Color.parseColor(bgInput);
            this.bgHover = Color.parseColor(bgHover);
            this.border = Color.parseColor(border);
            this.borderFocus = Color.parseColor(borderFocus);
            this.textMain = Color.parseColor(textMain);
            this.textMuted = Color.parseColor(textMuted);
            this.primary = Color.parseColor(primary);
            this.success = Color.parseColor(success);
            this.warning = Color.parseColor(warning);
            this.danger = Color.parseColor(danger);
            this.purple = Color.parseColor(purple);
            this.cyan = Color.parseColor(cyan);
        }
    }

    private static final Map<String, Palette> PALETTES = new HashMap<>();

    static {
        PALETTES.put("github-dark", new Palette("#0d1117", "#161b22", "#1c2128", "#0d1117", "#21262d", "#30363d", "#58a6ff", "#e6edf3", "#8b949e", "#2f81f7", "#3fb950", "#d29922", "#f85149", "#a371f7", "#39c5cf"));
        PALETTES.put("github-light", new Palette("#f6f8fa", "#ffffff", "#ffffff", "#f6f8fa", "#eaeef2", "#d0d7de", "#0969da", "#1f2328", "#59636e", "#0969da", "#1a7f37", "#9a6700", "#cf222e", "#8250df", "#1b7c83"));
        PALETTES.put("darcula", new Palette("#2b2b2b", "#3c3f41", "#313335", "#232525", "#4b4eaf", "#515151", "#cc7832", "#a9b7c6", "#808080", "#cc7832", "#6a8759", "#bbb529", "#bc3f3c", "#9876aa", "#6897bb"));
        PALETTES.put("dracula", new Palette("#282a36", "#21222c", "#343746", "#191a21", "#44475a", "#6272a4", "#bd93f9", "#f8f8f2", "#bfbfbf", "#bd93f9", "#50fa7b", "#f1fa8c", "#ff5555", "#ff79c6", "#8be9fd"));
        PALETTES.put("monokai", new Palette("#272822", "#1e1f1c", "#2d2e27", "#1b1c18", "#3e3d32", "#49483e", "#a6e22e", "#f8f8f2", "#a59f85", "#66d9ef", "#a6e22e", "#e6db74", "#f92672", "#ae81ff", "#66d9ef"));
        PALETTES.put("nord", new Palette("#2e3440", "#242933", "#3b4252", "#242933", "#434c5e", "#4c566a", "#88c0d0", "#eceff4", "#d8dee9", "#88c0d0", "#a3be8c", "#ebcb8b", "#bf616a", "#b48ead", "#8fbcbb"));
        PALETTES.put("tokyo-night", new Palette("#1a1b26", "#16161e", "#24283b", "#15161e", "#292e42", "#3b4261", "#7aa2f7", "#c0caf5", "#a9b1d6", "#7aa2f7", "#9ece6a", "#e0af68", "#f7768e", "#bb9af7", "#7dcfff"));
        PALETTES.put("catppuccin-mocha", new Palette("#1e1e2e", "#181825", "#313244", "#11111b", "#45475a", "#45475a", "#cba6f7", "#cdd6f4", "#a6adc8", "#89b4fa", "#a6e3a1", "#f9e2af", "#f38ba8", "#cba6f7", "#94e2d5"));
        PALETTES.put("catppuccin-latte", new Palette("#eff1f5", "#e6e9ef", "#ffffff", "#dce0e8", "#ccd0da", "#bcc0cc", "#1e66f5", "#4c4f69", "#5c5f77", "#1e66f5", "#40a02b", "#df8e1d", "#d20f39", "#8839ef", "#179299"));
        PALETTES.put("gruvbox-dark", new Palette("#282828", "#1d2021", "#32302f", "#1d2021", "#3c3836", "#504945", "#fabd2f", "#ebdbb2", "#bdae93", "#83a598", "#b8bb26", "#fabd2f", "#fb4934", "#d3869b", "#8ec07c"));
        PALETTES.put("solarized-dark", new Palette("#002b36", "#073642", "#0b3c49", "#00212b", "#134b5b", "#2aa198", "#268bd2", "#eee8d5", "#93a1a1", "#268bd2", "#859900", "#b58900", "#dc322f", "#6c71c4", "#2aa198"));
        PALETTES.put("one-dark", new Palette("#282c34", "#21252b", "#2c313a", "#1b1d23", "#323842", "#3e4451", "#61afef", "#abb2bf", "#828997", "#61afef", "#98c379", "#e5c07b", "#e06c75", "#c678dd", "#56b6c2"));
        PALETTES.put("rose-pine", new Palette("#191724", "#1f1d2e", "#26233a", "#14121d", "#312e47", "#403d52", "#c4a7e7", "#e0def4", "#908caa", "#c4a7e7", "#9ccfd8", "#f6c177", "#eb6f92", "#c4a7e7", "#9ccfd8"));
        PALETTES.put("kanagawa", new Palette("#1f1f28", "#16161d", "#2a2a37", "#16161d", "#363646", "#54546d", "#7e9cd8", "#dcd7ba", "#c8c093", "#7e9cd8", "#98bb6c", "#e6c384", "#e46876", "#957fb8", "#7aa89f"));
        PALETTES.put("everforest", new Palette("#2d353b", "#232a2e", "#343f44", "#232a2e", "#3d484d", "#4f585e", "#a7c080", "#d3c6aa", "#9da9a0", "#a7c080", "#a7c080", "#dbbc7f", "#e67e80", "#d699b6", "#83c092"));
    }

    public static Palette getPalette(String themeId) {
        if (themeId != null && PALETTES.containsKey(themeId)) {
            return PALETTES.get(themeId);
        }
        return PALETTES.get("github-dark");
    }

    public static float getCornerRadiusDp(String uiStyle, boolean isButton) {
        if ("sharp-terminal".equals(uiStyle)) return 0f;
        if ("neubrutalist".equals(uiStyle)) return 2f;
        if ("ide-compact".equals(uiStyle)) return 4f;
        if ("android".equals(uiStyle)) return isButton ? 24f : 16f;
        if ("ios".equals(uiStyle)) return isButton ? 12f : 16f;
        return isButton ? 6f : 10f;
    }

    public static void styleRoundedBox(View view, int fillColor, int borderColor, float radiusDp, int borderWidthPx) {
        float density = view.getResources().getDisplayMetrics().density;
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(fillColor);
        gd.setCornerRadius(radiusDp * density);
        if (borderWidthPx > 0) {
            gd.setStroke(borderWidthPx, borderColor);
        }
        view.setBackground(gd);
    }
}
