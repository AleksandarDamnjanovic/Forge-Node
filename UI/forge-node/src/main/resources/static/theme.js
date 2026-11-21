/**
 * Forge Node — Global Open-Source Themes, UI Styles & Icon Packs Engine
 * Persists selected Color Palette, UI Style, and Icon Pack in AAU/script/themes/theme-config.conf
 * (and localStorage for instant first paint) across the entire Forge Node Web UI.
 */
(function () {
  const COLOR_STORAGE_KEY = "forge_node_color_theme_v1";
  const STYLE_STORAGE_KEY = "forge_node_ui_style_v1";
  const ICON_STORAGE_KEY = "forge_node_icon_pack_v1";

  const THEMES = {
    "github-dark": {
      label: "GitHub Dark (Default)",
      scheme: "dark",
      vars: {
        "--bg-app": "#0d1117",
        "--bg-panel": "#161b22",
        "--bg-card": "#1c2128",
        "--bg-input": "#0d1117",
        "--bg-hover": "#21262d",
        "--bg-selected": "#1f2937",
        "--border": "#30363d",
        "--border-focus": "#58a6ff",
        "--text-main": "#e6edf3",
        "--text-muted": "#8b949e",
        "--text-dim": "#6e7681",
        "--primary": "#2f81f7",
        "--primary-hover": "#388bfd",
        "--success": "#3fb950",
        "--warning": "#d29922",
        "--danger": "#f85149",
        "--purple": "#a371f7",
        "--cyan": "#39c5cf",
        "--orange": "#db6d28"
      }
    },
    "github-light": {
      label: "GitHub Light",
      scheme: "light",
      vars: {
        "--bg-app": "#f6f8fa",
        "--bg-panel": "#ffffff",
        "--bg-card": "#ffffff",
        "--bg-input": "#f6f8fa",
        "--bg-hover": "#eaeef2",
        "--bg-selected": "#ddf4ff",
        "--border": "#d0d7de",
        "--border-focus": "#0969da",
        "--text-main": "#1f2328",
        "--text-muted": "#59636e",
        "--text-dim": "#818b98",
        "--primary": "#0969da",
        "--primary-hover": "#0550ae",
        "--success": "#1a7f37",
        "--warning": "#9a6700",
        "--danger": "#cf222e",
        "--purple": "#8250df",
        "--cyan": "#1b7c83",
        "--orange": "#bc4c00"
      }
    },
    "darcula": {
      label: "JetBrains Darcula",
      scheme: "dark",
      vars: {
        "--bg-app": "#2b2b2b",
        "--bg-panel": "#3c3f41",
        "--bg-card": "#313335",
        "--bg-input": "#232525",
        "--bg-hover": "#4b4eaf",
        "--bg-selected": "#214283",
        "--border": "#515151",
        "--border-focus": "#cc7832",
        "--text-main": "#a9b7c6",
        "--text-muted": "#808080",
        "--text-dim": "#606366",
        "--primary": "#cc7832",
        "--primary-hover": "#e08942",
        "--success": "#6a8759",
        "--warning": "#bbb529",
        "--danger": "#bc3f3c",
        "--purple": "#9876aa",
        "--cyan": "#6897bb",
        "--orange": "#cc7832"
      }
    },
    "dracula": {
      label: "Dracula",
      scheme: "dark",
      vars: {
        "--bg-app": "#282a36",
        "--bg-panel": "#21222c",
        "--bg-card": "#343746",
        "--bg-input": "#191a21",
        "--bg-hover": "#44475a",
        "--bg-selected": "#44475a",
        "--border": "#6272a4",
        "--border-focus": "#bd93f9",
        "--text-main": "#f8f8f2",
        "--text-muted": "#bfbfbf",
        "--text-dim": "#6272a4",
        "--primary": "#bd93f9",
        "--primary-hover": "#ff79c6",
        "--success": "#50fa7b",
        "--warning": "#f1fa8c",
        "--danger": "#ff5555",
        "--purple": "#ff79c6",
        "--cyan": "#8be9fd",
        "--orange": "#ffb86c"
      }
    },
    "monokai": {
      label: "Monokai Pro",
      scheme: "dark",
      vars: {
        "--bg-app": "#272822",
        "--bg-panel": "#1e1f1c",
        "--bg-card": "#2d2e27",
        "--bg-input": "#1b1c18",
        "--bg-hover": "#3e3d32",
        "--bg-selected": "#49483e",
        "--border": "#49483e",
        "--border-focus": "#a6e22e",
        "--text-main": "#f8f8f2",
        "--text-muted": "#a59f85",
        "--text-dim": "#75715e",
        "--primary": "#66d9ef",
        "--primary-hover": "#a6e22e",
        "--success": "#a6e22e",
        "--warning": "#e6db74",
        "--danger": "#f92672",
        "--purple": "#ae81ff",
        "--cyan": "#66d9ef",
        "--orange": "#fd971f"
      }
    },
    "nord": {
      label: "Nord",
      scheme: "dark",
      vars: {
        "--bg-app": "#2e3440",
        "--bg-panel": "#242933",
        "--bg-card": "#3b4252",
        "--bg-input": "#242933",
        "--bg-hover": "#434c5e",
        "--bg-selected": "#4c566a",
        "--border": "#4c566a",
        "--border-focus": "#88c0d0",
        "--text-main": "#eceff4",
        "--text-muted": "#d8dee9",
        "--text-dim": "#9099ab",
        "--primary": "#88c0d0",
        "--primary-hover": "#81a1c1",
        "--success": "#a3be8c",
        "--warning": "#ebcb8b",
        "--danger": "#bf616a",
        "--purple": "#b48ead",
        "--cyan": "#8fbcbb",
        "--orange": "#d08770"
      }
    },
    "tokyo-night": {
      label: "Tokyo Night",
      scheme: "dark",
      vars: {
        "--bg-app": "#1a1b26",
        "--bg-panel": "#16161e",
        "--bg-card": "#24283b",
        "--bg-input": "#15161e",
        "--bg-hover": "#292e42",
        "--bg-selected": "#33467c",
        "--border": "#3b4261",
        "--border-focus": "#7aa2f7",
        "--text-main": "#c0caf5",
        "--text-muted": "#a9b1d6",
        "--text-dim": "#565f89",
        "--primary": "#7aa2f7",
        "--primary-hover": "#89ddff",
        "--success": "#9ece6a",
        "--warning": "#e0af68",
        "--danger": "#f7768e",
        "--purple": "#bb9af7",
        "--cyan": "#7dcfff",
        "--orange": "#ff9e64"
      }
    },
    "catppuccin-mocha": {
      label: "Catppuccin Mocha",
      scheme: "dark",
      vars: {
        "--bg-app": "#1e1e2e",
        "--bg-panel": "#181825",
        "--bg-card": "#313244",
        "--bg-input": "#11111b",
        "--bg-hover": "#45475a",
        "--bg-selected": "#585b70",
        "--border": "#45475a",
        "--border-focus": "#cba6f7",
        "--text-main": "#cdd6f4",
        "--text-muted": "#a6adc8",
        "--text-dim": "#6c7086",
        "--primary": "#89b4fa",
        "--primary-hover": "#b4befe",
        "--success": "#a6e3a1",
        "--warning": "#f9e2af",
        "--danger": "#f38ba8",
        "--purple": "#cba6f7",
        "--cyan": "#94e2d5",
        "--orange": "#fab387"
      }
    },
    "catppuccin-latte": {
      label: "Catppuccin Latte",
      scheme: "light",
      vars: {
        "--bg-app": "#eff1f5",
        "--bg-panel": "#e6e9ef",
        "--bg-card": "#ffffff",
        "--bg-input": "#dce0e8",
        "--bg-hover": "#ccd0da",
        "--bg-selected": "#bcc0cc",
        "--border": "#bcc0cc",
        "--border-focus": "#1e66f5",
        "--text-main": "#4c4f69",
        "--text-muted": "#5c5f77",
        "--text-dim": "#7c7f93",
        "--primary": "#1e66f5",
        "--primary-hover": "#7287fd",
        "--success": "#40a02b",
        "--warning": "#df8e1d",
        "--danger": "#d20f39",
        "--purple": "#8839ef",
        "--cyan": "#179299",
        "--orange": "#fe640b"
      }
    },
    "gruvbox-dark": {
      label: "Gruvbox Dark",
      scheme: "dark",
      vars: {
        "--bg-app": "#282828",
        "--bg-panel": "#1d2021",
        "--bg-card": "#32302f",
        "--bg-input": "#1d2021",
        "--bg-hover": "#3c3836",
        "--bg-selected": "#504945",
        "--border": "#504945",
        "--border-focus": "#fabd2f",
        "--text-main": "#ebdbb2",
        "--text-muted": "#bdae93",
        "--text-dim": "#928374",
        "--primary": "#83a598",
        "--primary-hover": "#8ec07c",
        "--success": "#b8bb26",
        "--warning": "#fabd2f",
        "--danger": "#fb4934",
        "--purple": "#d3869b",
        "--cyan": "#8ec07c",
        "--orange": "#fe8019"
      }
    },
    "solarized-dark": {
      label: "Solarized Dark",
      scheme: "dark",
      vars: {
        "--bg-app": "#002b36",
        "--bg-panel": "#073642",
        "--bg-card": "#0b3c49",
        "--bg-input": "#00212b",
        "--bg-hover": "#134b5b",
        "--bg-selected": "#185a6d",
        "--border": "#2aa198",
        "--border-focus": "#268bd2",
        "--text-main": "#eee8d5",
        "--text-muted": "#93a1a1",
        "--text-dim": "#657b83",
        "--primary": "#268bd2",
        "--primary-hover": "#2aa198",
        "--success": "#859900",
        "--warning": "#b58900",
        "--danger": "#dc322f",
        "--purple": "#6c71c4",
        "--cyan": "#2aa198",
        "--orange": "#cb4b16"
      }
    },
    "one-dark": {
      label: "Atom One Dark",
      scheme: "dark",
      vars: {
        "--bg-app": "#282c34",
        "--bg-panel": "#21252b",
        "--bg-card": "#2c313a",
        "--bg-input": "#1b1d23",
        "--bg-hover": "#323842",
        "--bg-selected": "#3e4451",
        "--border": "#3e4451",
        "--border-focus": "#61afef",
        "--text-main": "#abb2bf",
        "--text-muted": "#828997",
        "--text-dim": "#5c6370",
        "--primary": "#61afef",
        "--primary-hover": "#56b6c2",
        "--success": "#98c379",
        "--warning": "#e5c07b",
        "--danger": "#e06c75",
        "--purple": "#c678dd",
        "--cyan": "#56b6c2",
        "--orange": "#d19a66"
      }
    },
    "rose-pine": {
      label: "Rosé Pine",
      scheme: "dark",
      vars: {
        "--bg-app": "#191724",
        "--bg-panel": "#1f1d2e",
        "--bg-card": "#26233a",
        "--bg-input": "#14121d",
        "--bg-hover": "#312e47",
        "--bg-selected": "#403d52",
        "--border": "#403d52",
        "--border-focus": "#c4a7e7",
        "--text-main": "#e0def4",
        "--text-muted": "#908caa",
        "--text-dim": "#6e6a86",
        "--primary": "#c4a7e7",
        "--primary-hover": "#ebbcba",
        "--success": "#9ccfd8",
        "--warning": "#f6c177",
        "--danger": "#eb6f92",
        "--purple": "#c4a7e7",
        "--cyan": "#9ccfd8",
        "--orange": "#ea9a97"
      }
    },
    "kanagawa": {
      label: "Kanagawa Wave",
      scheme: "dark",
      vars: {
        "--bg-app": "#1f1f28",
        "--bg-panel": "#16161d",
        "--bg-card": "#2a2a37",
        "--bg-input": "#16161d",
        "--bg-hover": "#363646",
        "--bg-selected": "#2d4f67",
        "--border": "#54546d",
        "--border-focus": "#7e9cd8",
        "--text-main": "#dcd7ba",
        "--text-muted": "#c8c093",
        "--text-dim": "#727169",
        "--primary": "#7e9cd8",
        "--primary-hover": "#7fb4ca",
        "--success": "#98bb6c",
        "--warning": "#e6c384",
        "--danger": "#e46876",
        "--purple": "#957fb8",
        "--cyan": "#7aa89f",
        "--orange": "#ffa066"
      }
    },
    "everforest": {
      label: "Everforest Dark",
      scheme: "dark",
      vars: {
        "--bg-app": "#2d353b",
        "--bg-panel": "#232a2e",
        "--bg-card": "#343f44",
        "--bg-input": "#232a2e",
        "--bg-hover": "#3d484d",
        "--bg-selected": "#475258",
        "--border": "#4f585e",
        "--border-focus": "#a7c080",
        "--text-main": "#d3c6aa",
        "--text-muted": "#9da9a0",
        "--text-dim": "#7a8478",
        "--primary": "#a7c080",
        "--primary-hover": "#83c092",
        "--success": "#a7c080",
        "--warning": "#dbbc7f",
        "--danger": "#e67e80",
        "--purple": "#d699b6",
        "--cyan": "#83c092",
        "--orange": "#e69875"
      }
    }
  };

  const UI_STYLES = {
    "forge-classic": {
      label: "Forge Classic (Default)",
      vars: {
        "--fn-radius-xs": "4px",
        "--fn-radius-sm": "6px",
        "--fn-radius-md": "8px",
        "--fn-radius-lg": "10px",
        "--fn-radius-pill": "999px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "none",
        "--fn-shadow-btn": "none",
        "--font-sans": '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, sans-serif',
        "--font-mono": '"JetBrains Mono", "Fira Code", "Cascadia Code", Consolas, monospace'
      }
    },
    "windows": {
      label: "Windows theme",
      vars: {
        "--fn-radius-xs": "3px",
        "--fn-radius-sm": "4px",
        "--fn-radius-md": "8px",
        "--fn-radius-lg": "8px",
        "--fn-radius-pill": "4px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "0 4px 16px rgba(0, 0, 0, 0.22), 0 1px 2px rgba(0, 0, 0, 0.14)",
        "--fn-shadow-btn": "inset 0 -1px 0 rgba(0, 0, 0, 0.22)",
        "--font-sans": '"Segoe UI Variable", "Segoe UI", system-ui, -apple-system, sans-serif',
        "--font-mono": '"Cascadia Code", "Cascadia Mono", Consolas, monospace'
      }
    },
    "ios": {
      label: "IOS theme",
      vars: {
        "--fn-radius-xs": "8px",
        "--fn-radius-sm": "12px",
        "--fn-radius-md": "16px",
        "--fn-radius-lg": "20px",
        "--fn-radius-pill": "999px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "0 10px 30px rgba(0, 0, 0, 0.24), 0 1px 3px rgba(0, 0, 0, 0.12)",
        "--fn-shadow-btn": "0 1px 2px rgba(0, 0, 0, 0.14)",
        "--font-sans": '-apple-system, BlinkMacSystemFont, "SF Pro Text", "SF Pro Display", "Helvetica Neue", sans-serif',
        "--font-mono": '"SF Mono", ui-monospace, Menlo, Monaco, monospace'
      }
    },
    "aws": {
      label: "AWS theme",
      vars: {
        "--fn-radius-xs": "2px",
        "--fn-radius-sm": "4px",
        "--fn-radius-md": "8px",
        "--fn-radius-lg": "8px",
        "--fn-radius-pill": "16px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "0 1px 4px rgba(0, 0, 0, 0.2)",
        "--fn-shadow-btn": "none",
        "--font-sans": '"Open Sans", "Helvetica Neue", Roboto, Arial, sans-serif',
        "--font-mono": 'Monaco, Menlo, Consolas, "Courier New", monospace'
      }
    },
    "android": {
      label: "Android theme",
      vars: {
        "--fn-radius-xs": "8px",
        "--fn-radius-sm": "12px",
        "--fn-radius-md": "16px",
        "--fn-radius-lg": "24px",
        "--fn-radius-pill": "999px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "0 2px 10px rgba(0, 0, 0, 0.2), 0 1px 3px rgba(0, 0, 0, 0.14)",
        "--fn-shadow-btn": "0 1px 3px rgba(0, 0, 0, 0.18)",
        "--font-sans": 'Roboto, "Google Sans", system-ui, -apple-system, sans-serif',
        "--font-mono": '"Roboto Mono", "JetBrains Mono", Consolas, monospace'
      }
    },
    "ide-compact": {
      label: "IDE Compact",
      vars: {
        "--fn-radius-xs": "2px",
        "--fn-radius-sm": "3px",
        "--fn-radius-md": "4px",
        "--fn-radius-lg": "4px",
        "--fn-radius-pill": "3px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "none",
        "--fn-shadow-btn": "none",
        "--font-sans": 'Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
        "--font-mono": '"JetBrains Mono", "Fira Code", Consolas, monospace'
      }
    },
    "sharp-terminal": {
      label: "Sharp Terminal",
      vars: {
        "--fn-radius-xs": "0px",
        "--fn-radius-sm": "0px",
        "--fn-radius-md": "0px",
        "--fn-radius-lg": "0px",
        "--fn-radius-pill": "0px",
        "--fn-border-width": "1px",
        "--fn-shadow-card": "none",
        "--fn-shadow-btn": "none",
        "--font-sans": '"JetBrains Mono", "Fira Code", Consolas, monospace',
        "--font-mono": '"JetBrains Mono", "Fira Code", Consolas, monospace'
      }
    },
    "neubrutalist": {
      label: "Neubrutalist",
      vars: {
        "--fn-radius-xs": "2px",
        "--fn-radius-sm": "2px",
        "--fn-radius-md": "2px",
        "--fn-radius-lg": "2px",
        "--fn-radius-pill": "2px",
        "--fn-border-width": "2px",
        "--fn-shadow-card": "4px 4px 0 var(--border)",
        "--fn-shadow-btn": "2px 2px 0 var(--border)",
        "--font-sans": 'Inter, "Segoe UI", Roboto, sans-serif',
        "--font-mono": '"JetBrains Mono", Consolas, monospace'
      }
    }
  };

  // Canonical icon key lookup from existing UI emojis
  const EMOJI_TO_KEY = {
    "📝": "script",
    "📡": "signal",
    "⚙️": "settings",
    "⚙": "settings",
    "🔌": "plug",
    "🔐": "lock",
    "🔒": "lock",
    "📜": "log",
    "🤖": "robot",
    "🏠": "home",
    "📂": "folder",
    "🗄": "server",
    "🗄️": "server",
    "💾": "save",
    "⬆": "upload",
    "⬆️": "upload",
    "⬇": "download",
    "⬇️": "download",
    "▶": "play",
    "▶️": "play",
    "⏹": "stop",
    "⏹️": "stop",
    "⏸": "pause",
    "⏸️": "pause",
    "🗑": "trash",
    "🗑️": "trash",
    "🔍": "search",
    "📦": "archive",
    "♻️": "restore",
    "♻": "restore",
    "🔄": "refresh",
    "↺": "refresh",
    "🔗": "link",
    "✓": "check",
    "✔": "check",
    "✕": "close",
    "⚠️": "warn",
    "⚠": "warn",
    "＋": "plus",
    "⇄": "sync",
    "📋": "copy",
    "←": "back"
  };

  const KEY_TO_EMOJI = {
    script: "📝",
    signal: "📡",
    settings: "⚙️",
    plug: "🔌",
    lock: "🔐",
    log: "📜",
    robot: "🤖",
    home: "🏠",
    folder: "📂",
    server: "🗄",
    save: "💾",
    upload: "⬆",
    download: "⬇",
    play: "▶",
    stop: "⏹",
    pause: "⏸",
    trash: "🗑",
    search: "🔍",
    archive: "📦",
    restore: "♻️",
    refresh: "🔄",
    link: "🔗",
    check: "✓",
    close: "✕",
    warn: "⚠️",
    plus: "＋",
    sync: "⇄",
    copy: "📋",
    back: "←"
  };

  const KEY_TO_ASCII = {
    script: "[FN]",
    signal: "[RF]",
    settings: "[CFG]",
    plug: "[IO]",
    lock: "[KEY]",
    log: "[LOG]",
    robot: "[AU]",
    home: "[HUB]",
    folder: "[DIR]",
    server: "[SRV]",
    save: "[SAV]",
    upload: "[UP]",
    download: "[DN]",
    play: "[>]",
    stop: "[#]",
    pause: "[||]",
    trash: "[DEL]",
    search: "[?]",
    archive: "[BAK]",
    restore: "[RST]",
    refresh: "[R]",
    link: "[LNK]",
    check: "[OK]",
    close: "[X]",
    warn: "[!]",
    plus: "[+]",
    sync: "[<>]",
    copy: "[CP]",
    back: "[<-]"
  };

  // Official open-source SVG icon packs (bundled offline; see /icons/LICENSES.txt)
  const REAL_SVG_PACKS = {"windows":{"script":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M8.75 11.5a.75.75 0 0 0 0 1.5h6.5a.75.75 0 0 0 0-1.5zm0 2.75a.75.75 0 0 0 0 1.5h6.5a.75.75 0 0 0 0-1.5zm0 2.75a.75.75 0 0 0 0 1.5h6.5a.75.75 0 0 0 0-1.5zm4.84-14.41L19.4 8.4A2 2 0 0 1 20 9.83V20a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V4c0-1.1.9-2 2-2h6.17c.52 0 1.05.22 1.42.59M18 20.5a.5.5 0 0 0 .5-.5V10H14a2 2 0 0 1-2-2V3.5H6a.5.5 0 0 0-.5.5v16c0 .27.22.5.5.5zm-.62-12L13.5 4.62V8c0 .28.22.5.5.5z\"/></svg>","signal":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M17.74 10.75q.9.9 1.5 2.07a.75.75 0 1 1-1.34.68 6.56 6.56 0 0 0-11.71-.02.75.75 0 1 1-1.34-.67q.57-1.14 1.5-2.06a8.06 8.06 0 0 1 11.4 0m-2.1 3.07q.68.7 1.08 1.58a.75.75 0 1 1-1.38.6A3.6 3.6 0 0 0 8.75 16a.75.75 0 1 1-1.37-.6q.4-.89 1.06-1.57a5.1 5.1 0 0 1 7.2 0m4.8-5.54q.79.77 1.42 1.7a.75.75 0 1 1-1.24.85q-.56-.82-1.23-1.5a10.45 10.45 0 0 0-16 1.5.75.75 0 0 1-1.23-.86q.65-.94 1.4-1.7a11.95 11.95 0 0 1 16.89 0m-7.38 8.16a1.5 1.5 0 1 1-2.12 2.12 1.5 1.5 0 0 1 2.12-2.12\"/></svg>","settings":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12.01 2.25q1.11.01 2.18.25c.32.07.55.33.59.65l.17 1.53a1.38 1.38 0 0 0 1.92 1.11l1.4-.61c.3-.13.64-.06.85.17a10 10 0 0 1 2.2 3.8c.1.3 0 .63-.26.82l-1.25.92a1.38 1.38 0 0 0 0 2.22l1.25.92c.26.19.36.52.27.82a10 10 0 0 1-2.2 3.8.75.75 0 0 1-.85.17l-1.4-.62a1.38 1.38 0 0 0-1.93 1.12l-.17 1.52a.75.75 0 0 1-.58.65 9.5 9.5 0 0 1-4.4 0 .75.75 0 0 1-.57-.65l-.17-1.52a1.38 1.38 0 0 0-1.93-1.11l-1.4.62a.75.75 0 0 1-.85-.18 10 10 0 0 1-2.2-3.8c-.1-.3 0-.63.26-.82l1.25-.92a1.38 1.38 0 0 0 0-2.22l-1.24-.92a.75.75 0 0 1-.28-.82 10 10 0 0 1 2.2-3.8c.23-.23.57-.3.86-.17l1.4.62c.4.17.86.15 1.25-.08.38-.22.63-.6.68-1.04l.17-1.53a.75.75 0 0 1 .58-.65q1.08-.24 2.2-.25m0 1.5q-.67 0-1.35.12l-.11.97a2.9 2.9 0 0 1-4.03 2.33l-.9-.4A8 8 0 0 0 4.29 9.1l.8.59a2.88 2.88 0 0 1 0 4.64l-.8.59a8 8 0 0 0 1.35 2.32l.9-.4a2.88 2.88 0 0 1 4.02 2.32l.1.99q1.35.22 2.7 0l.1-.99a2.88 2.88 0 0 1 4.02-2.32l.9.4a8 8 0 0 0 1.35-2.32l-.8-.59a2.88 2.88 0 0 1 0-4.64l.8-.59a8 8 0 0 0-1.35-2.32l-.9.4q-.55.24-1.15.24c-1.47 0-2.7-1.1-2.86-2.57l-.11-.97q-.68-.11-1.34-.12M12 8.25a3.75 3.75 0 1 1 0 7.5 3.75 3.75 0 0 1 0-7.5m0 1.5a2.25 2.25 0 1 0 0 4.5 2.25 2.25 0 0 0 0-4.5\"/></svg>","plug":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M19.49 5.57a5.97 5.97 0 0 1-1.9 8.96c-.64.35-1.42.14-1.94-.38l-5.8-5.8c-.52-.52-.73-1.3-.38-1.95a6 6 0 0 1 8.96-1.89l2.29-2.29a.75.75 0 1 1 1.06 1.06zm-2.02 7.26a4.5 4.5 0 1 0-6.3-6.3c-.27.35-.19.83.12 1.14l5.04 5.04c.31.3.8.39 1.14.12M3.28 21.78l2.3-2.29a5.97 5.97 0 0 0 8.95-1.9c.35-.64.14-1.42-.38-1.94l-5.8-5.8c-.52-.52-1.3-.73-1.95-.38a6 6 0 0 0-1.89 8.96l-2.29 2.29a.75.75 0 1 0 1.06 1.06m4.39-10.49 5.04 5.04c.3.31.39.8.12 1.14a4.5 4.5 0 1 1-6.3-6.3c.35-.27.83-.19 1.14.12\"/></svg>","lock":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12 1a5 5 0 0 1 5 5v2.01c1.68.13 3 1.53 3 3.24v7.5c0 1.8-1.46 3.25-3.25 3.25h-9.5A3.25 3.25 0 0 1 4 18.75v-7.5a3.25 3.25 0 0 1 3-3.24V6a5 5 0 0 1 5-5M7.25 9.5c-.97 0-1.75.78-1.75 1.75v7.5c0 .97.78 1.75 1.75 1.75h9.5c.97 0 1.75-.78 1.75-1.75v-7.5c0-.97-.78-1.75-1.75-1.75zM12 13.75a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5M12 2.5A3.5 3.5 0 0 0 8.5 6v2h7V6A3.5 3.5 0 0 0 12 2.5\"/></svg>","log":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M7 12.25a.75.75 0 1 1 1.5 0 .75.75 0 0 1-1.5 0m.75 2.25a.75.75 0 1 0 0 1.5.75.75 0 0 0 0-1.5M7 18.25a.75.75 0 1 1 1.5 0 .75.75 0 0 1-1.5 0m3.75-6.75a.75.75 0 0 0 0 1.5h5.5a.75.75 0 0 0 0-1.5zM10 15.25c0-.41.34-.75.75-.75h5.5a.75.75 0 0 1 0 1.5h-5.5a.75.75 0 0 1-.75-.75m.75 2.25a.75.75 0 0 0 0 1.5h5.5a.75.75 0 0 0 0-1.5zm8.66-9.09L13.6 2.6c-.37-.37-.9-.59-1.42-.59H6a2 2 0 0 0-2 2v16c0 1.1.9 2 2 2h12a2 2 0 0 0 2-2V9.83a2 2 0 0 0-.59-1.42M18.5 20a.5.5 0 0 1-.5.5H6a.5.5 0 0 1-.5-.5V4c0-.27.22-.5.5-.5h6V8c0 1.1.9 2 2 2h4.5zm-5-15.38 3.88 3.88H14a.5.5 0 0 1-.5-.5z\"/></svg>","robot":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M17.75 14C19 14 20 15 20 16.25v.9c0 1.1-.47 2.14-1.3 2.85q-2.36 2-6.7 2t-6.7-2A3.8 3.8 0 0 1 4 17.16v-.91C4 15 5.01 14 6.25 14zm0 1.5H6.25a.75.75 0 0 0-.75.75v.9c0 .66.29 1.29.79 1.71Q8.17 20.5 12 20.5q3.83-.01 5.72-1.64c.5-.43.78-1.05.78-1.7v-.91a.75.75 0 0 0-.75-.75M11.9 2h.1c.38 0 .7.28.74.65l.01.1v.75h3.5c1.24 0 2.25 1 2.25 2.25v4.5c0 1.25-1 2.25-2.25 2.25h-8.5c-1.24 0-2.25-1-2.25-2.25v-4.5c0-1.24 1-2.25 2.25-2.25h3.5v-.75c0-.38.28-.7.65-.74L12 2zm4.35 3h-8.5a.75.75 0 0 0-.75.75v4.5c0 .42.34.75.75.75h8.5c.41 0 .75-.33.75-.75v-4.5a.75.75 0 0 0-.75-.75m-6.5 1.5a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5m4.5 0a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5\"/></svg>","home":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M10.55 2.53c.84-.7 2.06-.7 2.9 0l6.75 5.7c.5.42.8 1.05.8 1.71v9.31c0 .97-.78 1.75-1.75 1.75h-3.5c-.97 0-1.75-.78-1.75-1.75v-5a.25.25 0 0 0-.25-.25h-3.5a.25.25 0 0 0-.25.25v5c0 .97-.78 1.75-1.75 1.75h-3.5C3.78 21 3 20.22 3 19.25v-9.3c0-.67.3-1.3.8-1.73zm1.93 1.15a.75.75 0 0 0-.96 0l-6.75 5.7a.8.8 0 0 0-.27.56v9.31c0 .14.11.25.25.25h3.5q.23-.01.25-.25v-5c0-.97.78-1.75 1.75-1.75h3.5c.97 0 1.75.78 1.75 1.75v5q.02.23.25.25h3.5q.23-.01.25-.25v-9.3q0-.36-.27-.58z\"/></svg>","folder":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M3.5 6.25c0-.97.78-1.75 1.75-1.75h2.88q.31 0 .53.22l2.06 2.06q.22.22.53.22h5.5c.97 0 1.75.78 1.75 1.75q0 .13.04.25H8.72c-1.34 0-2.58.71-3.25 1.87L3.5 14.28zM2 17.79A3.25 3.25 0 0 0 5.25 21h11.04c1.33 0 2.57-.72 3.24-1.88l3.03-5.25A3.25 3.25 0 0 0 19.96 9a1 1 0 0 0 .04-.25c0-1.8-1.45-3.25-3.25-3.25h-5.19L9.72 3.66c-.42-.42-1-.66-1.6-.66H5.26A3.25 3.25 0 0 0 2 6.25v11.54m6.72-7.3h11.03a1.75 1.75 0 0 1 1.51 2.63l-3.03 5.25c-.4.7-1.14 1.13-1.95 1.13H5.25a1.75 1.75 0 0 1-1.51-2.63l3.03-5.25c.4-.7 1.14-1.12 1.95-1.12\"/></svg>","server":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M9.25 6a.75.75 0 0 0 0 1.5h5.5a.75.75 0 0 0 0-1.5zM8.5 17.75c0-.41.34-.75.75-.75h5.5a.75.75 0 0 1 0 1.5h-5.5a.75.75 0 0 1-.75-.75M9.25 14a.75.75 0 0 0 0 1.5h5.5a.75.75 0 0 0 0-1.5zM6 5a3 3 0 0 1 3-3h6a3 3 0 0 1 3 3v14a3 3 0 0 1-3 3H9a3 3 0 0 1-3-3zm3-1.5c-.83 0-1.5.67-1.5 1.5v14c0 .83.67 1.5 1.5 1.5h6c.83 0 1.5-.67 1.5-1.5V5c0-.83-.67-1.5-1.5-1.5z\"/></svg>","save":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M3 5.75A2.75 2.75 0 0 1 5.75 3h9.96c.87 0 1.7.34 2.3.95L20.05 6c.6.6.95 1.43.95 2.3v9.96A2.75 2.75 0 0 1 18.25 21H5.75A2.75 2.75 0 0 1 3 18.25zM5.75 4.5c-.69 0-1.25.56-1.25 1.25v12.5c0 .69.56 1.25 1.25 1.25H6v-5.25C6 13.01 7 12 8.25 12h7.5c1.24 0 2.25 1 2.25 2.25v5.25h.25c.69 0 1.25-.56 1.25-1.25V8.29c0-.47-.18-.91-.51-1.24L16.95 5q-.4-.4-.95-.49v2.73c0 1.24-1 2.25-2.25 2.25h-4.5C8.01 9.5 7 8.5 7 7.25V4.5zm10.75 15v-5.25a.75.75 0 0 0-.75-.75h-7.5a.75.75 0 0 0-.75.75v5.25zm-8-15v2.75c0 .41.34.75.75.75h4.5c.41 0 .75-.34.75-.75V4.5z\"/></svg>","upload":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M18.25 3.51a.75.75 0 1 0 0-1.5h-13a.75.75 0 1 0 0 1.5zM11.65 22h.1c.38 0 .7-.28.74-.64l.01-.1V7.56l3.72 3.72c.27.27.68.29.98.07l.08-.07a.75.75 0 0 0 .07-.98l-.07-.08-5-5a.75.75 0 0 0-.97-.07l-.09.07-5 5a.75.75 0 0 0 .98 1.13l.08-.07L11 7.58v13.67c0 .38.28.7.65.75\"/></svg>","download":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M18.25 20.5a.75.75 0 1 1 0 1.5h-13a.75.75 0 1 1 0-1.5zm-6.6-18.49h.1c.38 0 .7.28.74.64l.01.1v13.7l3.72-3.73a.75.75 0 0 1 .98-.07l.08.07c.27.27.3.68.07.98l-.07.08-5 5a.75.75 0 0 1-.97.07l-.09-.07-5-5a.75.75 0 0 1 .98-1.13l.08.07L11 16.43V2.76c0-.38.28-.7.65-.75h.1z\"/></svg>","play":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M7.6 4.61a.75.75 0 0 0-1.1.66v13.46c0 .56.6.93 1.1.65l12.37-6.72a.75.75 0 0 0 0-1.32zM5 5.27c0-1.7 1.83-2.79 3.33-1.97l12.36 6.72a2.25 2.25 0 0 1 0 3.96L8.33 20.7A2.25 2.25 0 0 1 5 18.73z\"/></svg>","stop":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M19.25 4.5c.14 0 .25.11.25.25v14.5q-.02.23-.25.25H4.75a.25.25 0 0 1-.25-.25V4.75c0-.14.11-.25.25-.25zM4.75 3C3.78 3 3 3.78 3 4.75v14.5c0 .97.78 1.75 1.75 1.75h14.5c.97 0 1.75-.78 1.75-1.75V4.75C21 3.78 20.22 3 19.25 3z\"/></svg>","pause":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M6.25 3C5.01 3 4 4 4 5.25v13.5C4 19.99 5 21 6.25 21h2.5C9.99 21 11 20 11 18.75V5.25C11 4.01 10 3 8.75 3zM5.5 5.25c0-.41.34-.75.75-.75h2.5c.41 0 .75.34.75.75v13.5c0 .41-.34.75-.75.75h-2.5a.75.75 0 0 1-.75-.75zM15.25 3C14.01 3 13 4 13 5.25v13.5c0 1.24 1 2.25 2.25 2.25h2.5c1.24 0 2.25-1 2.25-2.25V5.25C20 4.01 19 3 17.75 3zm-.75 2.25c0-.41.34-.75.75-.75h2.5c.41 0 .75.34.75.75v13.5c0 .41-.34.75-.75.75h-2.5a.75.75 0 0 1-.75-.75z\"/></svg>","trash":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M10 5h4a2 2 0 1 0-4 0M8.5 5a3.5 3.5 0 1 1 7 0h5.75a.75.75 0 0 1 0 1.5h-1.32l-1.17 12.11A3.75 3.75 0 0 1 15.03 22H8.97a3.75 3.75 0 0 1-3.73-3.39L4.07 6.5H2.75a.75.75 0 0 1 0-1.5zm2 4.75a.75.75 0 0 0-1.5 0v7.5a.75.75 0 0 0 1.5 0zM14.25 9c.41 0 .75.34.75.75v7.5a.75.75 0 0 1-1.5 0v-7.5c0-.41.34-.75.75-.75m-7.52 9.47a2.25 2.25 0 0 0 2.24 2.03h6.06c1.15 0 2.12-.88 2.24-2.03L18.42 6.5H5.58z\"/></svg>","search":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M16.1 17.16a8 8 0 1 1 1.06-1.06l4.62 4.62a.75.75 0 1 1-1.06 1.06zM17.5 11a6.5 6.5 0 1 0-13 0 6.5 6.5 0 0 0 13 0\"/></svg>","archive":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M10.25 11a.75.75 0 0 0 0 1.5h3.5a.75.75 0 0 0 0-1.5zM3 5.25C3 4.01 4 3 5.25 3h13.5C19.99 3 21 4 21 5.25v1.5c0 .78-.4 1.47-1 1.87v8.63A3.75 3.75 0 0 1 16.25 21h-8.5A3.75 3.75 0 0 1 4 17.25V8.62c-.6-.4-1-1.09-1-1.87zM5.5 9v8.25c0 1.24 1 2.25 2.25 2.25h8.5c1.24 0 2.25-1 2.25-2.25V9zm-.25-4.5a.75.75 0 0 0-.75.75v1.5c0 .41.34.75.75.75h13.5c.41 0 .75-.34.75-.75v-1.5a.75.75 0 0 0-.75-.75z\"/></svg>","restore":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12 4.5a7.5 7.5 0 1 1-7.42 6.4c.07-.46-.26-.9-.72-.9-.37 0-.7.26-.76.62Q3 11.3 3 12a9 9 0 1 0 3-6.7V4.24a.75.75 0 0 0-1.5 0v3c0 .41.34.75.75.75h3a.75.75 0 0 0 0-1.5H6.9a7.5 7.5 0 0 1 5.1-2\"/></svg>","refresh":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M16.25 5.18c-.25.33-.19.8.14 1.05a7.24 7.24 0 0 1-3.6 12.98l.68-.68a.75.75 0 0 0-.98-1.13l-.08.07-2 2a.75.75 0 0 0-.07.98l.07.08 2 2a.75.75 0 0 0 1.13-.98l-.07-.08-.75-.75A8.75 8.75 0 0 0 17.3 5.04a.75.75 0 0 0-1.05.14m-5.72-3.71c-.3.3-.3.77 0 1.06l.75.75a8.75 8.75 0 0 0-4.85 15.47.75.75 0 1 0 .96-1.16A7.23 7.23 0 0 1 11.2 4.8l-.68.68a.75.75 0 1 0 1.06 1.06l2-2c.3-.3.3-.77 0-1.06l-2-2a.75.75 0 0 0-1.06 0\"/></svg>","link":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M9.25 7a.75.75 0 0 1 .11 1.5H7a3.5 3.5 0 0 0-.2 7h2.45a.75.75 0 0 1 .11 1.5H7a5 5 0 0 1-.25-10h2.5M17 7a5 5 0 0 1 .25 10h-2.5a.75.75 0 0 1-.11-1.5H17a3.5 3.5 0 0 0 .2-7h-2.45a.75.75 0 0 1-.11-1.5H17M7 11.25h10a.75.75 0 0 1 .1 1.5H7a.75.75 0 0 1-.1-1.5zh10z\"/></svg>","check":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M4.53 12.97a.75.75 0 0 0-1.06 1.06l4.5 4.5c.3.3.77.3 1.06 0l11-11a.75.75 0 0 0-1.06-1.06L8.5 16.94z\"/></svg>","close":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"m4.4 4.55.07-.08a.75.75 0 0 1 .98-.07l.08.07L12 10.94l6.47-6.47a.75.75 0 1 1 1.06 1.06L13.06 12l6.47 6.47c.27.27.3.68.07.98l-.07.08a.75.75 0 0 1-.98.07l-.08-.07L12 13.06l-6.47 6.47a.75.75 0 0 1-1.06-1.06L10.94 12 4.47 5.53a.75.75 0 0 1-.07-.98l.07-.08z\"/></svg>","warn":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M9.14 3.7a3.25 3.25 0 0 1 5.72 0l6.74 12.5a3.25 3.25 0 0 1-2.86 4.8H5.25a3.25 3.25 0 0 1-2.86-4.8zm4.4.72a1.75 1.75 0 0 0-3.08 0L3.7 16.92a1.75 1.75 0 0 0 1.54 2.58h13.5a1.75 1.75 0 0 0 1.53-2.58zM12 15a1 1 0 1 1 0 2 1 1 0 0 1 0-2m0-7.5c.41 0 .75.34.75.75v4.5a.75.75 0 0 1-1.5 0v-4.5c0-.41.34-.75.75-.75\"/></svg>","plus":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12 3.25c.41 0 .75.34.75.75v7.25H20a.75.75 0 0 1 0 1.5h-7.25V20a.75.75 0 0 1-1.5 0v-7.25H4a.75.75 0 0 1 0-1.5h7.25V4c0-.41.34-.75.75-.75\"/></svg>","sync":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M9.03 6.47c.3.3.3.77 0 1.06L5.56 11h12.88l-3.47-3.47a.75.75 0 0 1 1.06-1.06l4.75 4.75c.3.3.3.77 0 1.06l-4.75 4.75a.75.75 0 1 1-1.06-1.06l3.47-3.47H5.56l3.47 3.47a.75.75 0 1 1-1.06 1.06l-4.75-4.75a.75.75 0 0 1 0-1.06l4.75-4.75c.3-.3.77-.3 1.06 0\"/></svg>","copy":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M8.5 8.5H5.25c-.97 0-1.75.78-1.75 1.75v8.5c0 .97.78 1.75 1.75 1.75h6c.97 0 1.75-.78 1.75-1.75V18h1.5v.75c0 1.8-1.46 3.25-3.25 3.25h-6A3.25 3.25 0 0 1 2 18.75v-8.5C2 8.45 3.46 7 5.25 7H8.5zM18.75 2C20.55 2 22 3.46 22 5.25v8.5c0 1.8-1.46 3.25-3.25 3.25h-6a3.25 3.25 0 0 1-3.25-3.25v-8.5C9.5 3.45 10.96 2 12.75 2zm-6 1.5c-.97 0-1.75.78-1.75 1.75v8.5c0 .97.78 1.75 1.75 1.75h6c.97 0 1.75-.78 1.75-1.75v-8.5c0-.97-.78-1.75-1.75-1.75z\"/></svg>","back":"<svg class=\"fn-svg-icon fn-svg-fluent\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M10.73 19.8a.75.75 0 0 0 1.04-1.1l-6.25-5.95h14.73a.75.75 0 0 0 0-1.5H5.52l6.25-5.95a.75.75 0 0 0-1.04-1.1l-7.42 7.08a1 1 0 0 0 0 1.44z\"/></svg>"},"ios":{"script":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M416 221.25V416a48 48 0 0 1-48 48H144a48 48 0 0 1-48-48V96a48 48 0 0 1 48-48h98.75a32 32 0 0 1 22.62 9.37l141.26 141.26a32 32 0 0 1 9.37 22.62Z\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M256 56v120a32 32 0 0 0 32 32h120M176 288h160M176 368h160\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","signal":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><circle cx=\"256\" cy=\"256.02\" r=\"32\"/><path d=\"M184.25 192.25a96 96 0 0 0 0 127.52M327.77 319.77a96 96 0 0 0 0-127.52M133.28 141.28a168 168 0 0 0 0 229.44M378.72 370.72a168 168 0 0 0 0-229.44\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M435 416a240.34 240.34 0 0 0 0-320M77 96a240.34 240.34 0 0 0 0 320\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","settings":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M262.29 192.31a64 64 0 1 0 57.4 57.4 64.13 64.13 0 0 0-57.4-57.4M416.39 256a154 154 0 0 1-1.53 20.79l45.21 35.46a10.81 10.81 0 0 1 2.45 13.75l-42.77 74a10.81 10.81 0 0 1-13.14 4.59l-44.9-18.08a16.11 16.11 0 0 0-15.17 1.75A164.5 164.5 0 0 1 325 400.8a15.94 15.94 0 0 0-8.82 12.14l-6.73 47.89a11.08 11.08 0 0 1-10.68 9.17h-85.54a11.11 11.11 0 0 1-10.69-8.87l-6.72-47.82a16.07 16.07 0 0 0-9-12.22 155 155 0 0 1-21.46-12.57 16 16 0 0 0-15.11-1.71l-44.89 18.07a10.81 10.81 0 0 1-13.14-4.58l-42.77-74a10.8 10.8 0 0 1 2.45-13.75l38.21-30a16.05 16.05 0 0 0 6-14.08c-.36-4.17-.58-8.33-.58-12.5s.21-8.27.58-12.35a16 16 0 0 0-6.07-13.94l-38.19-30A10.81 10.81 0 0 1 49.48 186l42.77-74a10.81 10.81 0 0 1 13.14-4.59l44.9 18.08a16.11 16.11 0 0 0 15.17-1.75A164.5 164.5 0 0 1 187 111.2a15.94 15.94 0 0 0 8.82-12.14l6.73-47.89A11.08 11.08 0 0 1 213.23 42h85.54a11.11 11.11 0 0 1 10.69 8.87l6.72 47.82a16.07 16.07 0 0 0 9 12.22 155 155 0 0 1 21.46 12.57 16 16 0 0 0 15.11 1.71l44.89-18.07a10.81 10.81 0 0 1 13.14 4.58l42.77 74a10.8 10.8 0 0 1-2.45 13.75l-38.21 30a16.05 16.05 0 0 0-6.05 14.08c.33 4.14.55 8.3.55 12.47\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","plug":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M315.27 33 96 304h128l-31.51 173.23a2.36 2.36 0 0 0 2.33 2.77h0a2.36 2.36 0 0 0 1.89-.95L416 208H288l31.66-173.25a2.45 2.45 0 0 0-2.44-2.75h0a2.42 2.42 0 0 0-1.95 1\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","lock":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M336 208v-95a80 80 0 0 0-160 0v95\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><rect width=\"320\" height=\"272\" x=\"96\" y=\"208\" rx=\"48\" ry=\"48\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","log":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><rect width=\"320\" height=\"416\" x=\"96\" y=\"48\" rx=\"48\" ry=\"48\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M176 128h160M176 208h160M176 288h80\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","robot":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><rect width=\"352\" height=\"352\" x=\"80\" y=\"80\" rx=\"48\" ry=\"48\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><rect width=\"224\" height=\"224\" x=\"144\" y=\"144\" rx=\"16\" ry=\"16\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M256 80V48M336 80V48M176 80V48M256 464v-32M336 464v-32M176 464v-32M432 256h32M432 336h32M432 176h32M48 256h32M48 336h32M48 176h32\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","home":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M80 212v236a16 16 0 0 0 16 16h96V328a24 24 0 0 1 24-24h80a24 24 0 0 1 24 24v136h96a16 16 0 0 0 16-16V212\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M480 256 266.89 52c-5-5.28-16.69-5.34-21.78 0L32 256M400 179V64h-48v69\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","folder":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M64 192v-72a40 40 0 0 1 40-40h75.89a40 40 0 0 1 22.19 6.72l27.84 18.56a40 40 0 0 0 22.19 6.72H408a40 40 0 0 1 40 40v40\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M479.9 226.55 463.68 392a40 40 0 0 1-39.93 40H88.25a40 40 0 0 1-39.93-40L32.1 226.55A32 32 0 0 1 64 192h384.1a32 32 0 0 1 31.8 34.55\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","server":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" fill=\"none\" viewBox=\"0 0 512 512\"><ellipse cx=\"256\" cy=\"112\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32\" rx=\"176\" ry=\"80\"/><path stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32\" d=\"M432 112v288c0 44.183-78.798 80-176 80S80 444.183 80 400V112\"/><path stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32\" d=\"M432 256c0 44.183-78.798 80-176 80S80 300.183 80 256\"/></svg>","save":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M380.93 57.37A32 32 0 0 0 358.3 48H94.22A46.21 46.21 0 0 0 48 94.22v323.56A46.21 46.21 0 0 0 94.22 464h323.56A46.36 46.36 0 0 0 464 417.78V153.7a32 32 0 0 0-9.37-22.63ZM256 416a64 64 0 1 1 64-64 63.92 63.92 0 0 1-64 64m48-224H112a16 16 0 0 1-16-16v-64a16 16 0 0 1 16-16h192a16 16 0 0 1 16 16v64a16 16 0 0 1-16 16\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","upload":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M320 367.79h76c55 0 100-29.21 100-83.6s-53-81.47-96-83.6c-8.89-85.06-71-136.8-144-136.8-69 0-113.44 45.79-128 91.2-60 5.7-112 43.88-112 106.4s54 106.4 120 106.4h56\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"m320 255.79-64-64-64 64M256 448.21V207.79\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","download":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M320 336h76c55 0 100-21.21 100-75.6s-53-73.47-96-75.6C391.11 99.74 329 48 256 48c-69 0-113.44 45.79-128 91.2-60 5.7-112 35.88-112 98.4S70 336 136 336h56M192 400.1l64 63.9 64-63.9M256 224v224.03\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","play":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M112 111v290c0 17.44 17 28.52 31 20.16l247.9-148.37c12.12-7.25 12.12-26.33 0-33.58L143 90.84c-14-8.36-31 2.72-31 20.16Z\" fill=\"none\" stroke=\"currentColor\" stroke-miterlimit=\"10\" stroke-width=\"32px\"/></svg>","stop":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><rect width=\"320\" height=\"320\" x=\"96\" y=\"96\" rx=\"24\" ry=\"24\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","pause":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M176 96h16v320h-16zM320 96h16v320h-16z\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","trash":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"m112 112 20 320c.95 18.49 14.4 32 32 32h184c17.67 0 30.87-13.51 32-32l20-320\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"M80 112h352\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-miterlimit=\"10\" stroke-width=\"32px\"/><path d=\"M192 112V72h0a23.93 23.93 0 0 1 24-24h80a23.93 23.93 0 0 1 24 24h0v40M256 176v224M184 176l8 224M328 176l-8 224\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","search":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M221.09 64a157.09 157.09 0 1 0 157.09 157.09A157.1 157.1 0 0 0 221.09 64Z\" fill=\"none\" stroke=\"currentColor\" stroke-miterlimit=\"10\" stroke-width=\"32px\"/><path d=\"M338.29 338.29 448 448\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-miterlimit=\"10\" stroke-width=\"32px\"/></svg>","archive":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M80 152v256a40.12 40.12 0 0 0 40 40h272a40.12 40.12 0 0 0 40-40V152\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><rect width=\"416\" height=\"80\" x=\"48\" y=\"64\" rx=\"28\" ry=\"28\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"m320 304-64 64-64-64M256 345.89V224\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","restore":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M240 424v-96c116.4 0 159.39 33.76 208 96 0-119.23-39.57-240-208-240V88L64 256Z\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","refresh":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M320 146s24.36-12-64-12a160 160 0 1 0 160 160\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-miterlimit=\"10\" stroke-width=\"32px\"/><path d=\"m256 58 80 80-80 80\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","link":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M208 352h-64a96 96 0 0 1 0-192h64M304 160h64a96 96 0 0 1 0 192h-64M163.29 256h187.42\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"36px\"/></svg>","check":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M416 128 192 384l-96-96\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","close":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M368 368 144 144M368 144 144 368\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","warn":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M85.57 446.25h340.86a32 32 0 0 0 28.17-47.17L284.18 82.58c-12.09-22.44-44.27-22.44-56.36 0L57.4 399.08a32 32 0 0 0 28.17 47.17\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"m250.26 195.39 5.74 122 5.73-121.95a5.74 5.74 0 0 0-5.79-6h0a5.74 5.74 0 0 0-5.68 5.95\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path fill=\"currentColor\" d=\"M256 397.25a20 20 0 1 1 20-20 20 20 0 0 1-20 20\"/></svg>","plus":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M256 112v288M400 256H112\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","sync":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"m304 48 112 112-112 112M398.87 160H96M208 464 96 352l112-112M114 352h302\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","copy":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><rect width=\"336\" height=\"336\" x=\"128\" y=\"128\" rx=\"57\" ry=\"57\" fill=\"none\" stroke=\"currentColor\" stroke-linejoin=\"round\" stroke-width=\"32px\"/><path d=\"m383.5 128 .5-24a56.16 56.16 0 0 0-56-56H112a64.19 64.19 0 0 0-64 64v216a56.16 56.16 0 0 0 56 56h24\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"32px\"/></svg>","back":"<svg class=\"fn-svg-icon fn-svg-ionicon\" aria-hidden=\"true\" viewBox=\"0 0 512 512\" fill=\"currentColor\"><path d=\"M244 400 100 256l144-144M120 256h292\" fill=\"none\" stroke=\"currentColor\" stroke-linecap=\"round\" stroke-linejoin=\"round\" stroke-width=\"48px\"/></svg>"},"aws":{"script":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M5 4 1 8l4 4M11 4l4 4-4 4\" class=\"stroke-linejoin-round\"/><path d=\"M7 13 9 3\"/></svg>","signal":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M15 5H5v7h10V5Z\" class=\"stroke-linejoin-round awsui-icon-front\"/><path d=\"M11 3V1.01L1.01 1 1 8h1.998\" class=\"stroke-linejoin-round awsui-icon-back\"/><path d=\"M10 12v3M7 15h6\" class=\"stroke-linejoin-round awsui-icon-front\"/></svg>","settings":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M6.11 1.729c.07-.42.44-.729.86-.729h2.02c.43 0 .79.31.86.729l.17.999c.05.29.24.529.5.679.06.03.11.06.17.1.25.15.56.2.84.1l.95-.35c.4-.15.85 0 1.07.38l1.01 1.747c.21.37.13.839-.2 1.108l-.78.64c-.23.189-.34.479-.33.768v.2c0 .29.11.579.33.769l.78.639c.33.27.42.739.2 1.108l-1.01 1.748c-.21.37-.66.529-1.06.38l-.95-.35a.966.966 0 0 0-.84.1c-.06.03-.11.07-.17.1-.26.14-.45.389-.5.679l-.17.998A.878.878 0 0 1 9 15H6.98a.87.87 0 0 1-.86-.729l-.17-.998a.988.988 0 0 0-.5-.68c-.06-.03-.11-.06-.17-.1a.996.996 0 0 0-.84-.1l-.95.35c-.4.15-.85 0-1.06-.38l-1.01-1.747a.873.873 0 0 1 .2-1.108l.78-.64c.23-.189.34-.479.33-.768v-.2c0-.3-.11-.579-.33-.769l-.78-.639a.861.861 0 0 1-.2-1.108l1.01-1.748c.21-.37.66-.529 1.07-.38l.95.35c.28.1.58.06.84-.1.06-.03.11-.07.17-.1.26-.14.45-.379.5-.678l.15-1Z\" class=\"stroke-linecap-round stroke-linejoin-round\"/><path d=\"M10 8c0 1.1-.9 2-2 2s-2-.9-2-2 .9-2 2-2 2 .9 2 2Z\" class=\"stroke-linecap-round stroke-linejoin-round\"/></svg>","plug":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M15 1H1v14h14V1Z\" class=\"stroke-linejoin-round\"/><path d=\"M12 11H9\" class=\"stroke-linejoin-round awsui-icon-cursor\"/><path d=\"M4.71 11.3 8 8 4.71 4.71\" class=\"stroke-linejoin-round\"/></svg>","lock":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M12 7H4a1 1 0 0 0-1 1v6a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1V8a1 1 0 0 0-1-1Z\" class=\"stroke-linejoin-round\"/><g class=\"awsui-icon-clip\"/><path d=\"M5 7V4c0-1.65 1.35-3 3-3s3 1.35 3 3v3\" class=\"stroke-linejoin-round awsui-icon-shackle\"/></svg>","log":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M15 1H1v14h14V1Z\" class=\"stroke-linejoin-round\"/><path d=\"M9 10H4\" class=\"stroke-linejoin-round awsui-icon-line-bottom\"/><path d=\"M12 6H4\" class=\"stroke-linejoin-round awsui-icon-line-top\"/></svg>","robot":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M6.15 10.365 8 15.005l1.86-4.64 4.64-1.86-4.64-1.85L8 2.005l-1.85 4.65-4.65 1.85 4.65 1.86Z\" class=\"stroke-linejoin-round\"/><path d=\"M2.38 4.915c.02.05.07.08.12.08.05 0 .12-.08.12-.08l.66-1.64 1.64-.66a.13.13 0 0 0 .08-.12c0-.05-.08-.12-.08-.12l-1.64-.66-.66-1.64c-.04-.1-.2-.1-.24 0l-.66 1.64-1.64.66a.13.13 0 0 0-.08.12c0 .05.08.12.08.12l1.64.66.66 1.64Z\" class=\"filled no-stroke awsui-icon-sparkle\"/></svg>","home":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M6 10H2v4h4v-4Z\" class=\"stroke-linejoin-round awsui-icon-cell-bl\"/><path d=\"M14 10h-4v4h4v-4Z\" class=\"stroke-linejoin-round awsui-icon-cell-br\"/><path d=\"M6 2H2v4h4V2Z\" class=\"stroke-linejoin-round awsui-icon-cell-tl\"/><path d=\"M14 2h-4v4h4V2Z\" class=\"stroke-linejoin-round awsui-icon-cell-tr\"/></svg>","folder":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M2 7V2h6l1 2h5c.55 0 1 .45 1 1v8c0 .55-.45 1-1 1h-1\" class=\"stroke-linejoin-round\"/><path d=\"M1 7h10\" class=\"awsui-icon-flap-top\"/><path d=\"M3 14h11\" class=\"awsui-icon-flap-bottom\"/><path d=\"m1 7 2 7\" class=\"stroke-linecap-round awsui-icon-flap-left\"/><path d=\"m11 7 3 7\" class=\"stroke-linecap-round awsui-icon-flap-right\"/></svg>","server":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M15 1H1v14h14V1Z\" class=\"stroke-linejoin-round\"/><svg x=\"1\" y=\"1\" width=\"14\" height=\"14\" viewBox=\"1 1 14 14\" overflow=\"hidden\"/><path d=\"M11.5 4h-4a.5.5 0 0 0-.5.5v7a.5.5 0 0 0 .5.5h4a.5.5 0 0 0 .5-.5v-7a.5.5 0 0 0-.5-.5Z\" class=\"filled no-stroke awsui-icon-panel\"/></svg>","save":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"m1 9 4 4L15 2\" class=\"stroke-linejoin-round\"/></svg>","upload":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M1 1h14\" class=\"stroke-linejoin-round\"/><path d=\"M13 10 8 5l-5 5M8 6v9\" class=\"stroke-linejoin-round awsui-icon-arrow\"/></svg>","download":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M1 15h14\" class=\"stroke-linejoin-round\"/><path d=\"m13 6-5 5-5-5M8 10V1\" class=\"stroke-linejoin-round awsui-icon-arrow\"/></svg>","play":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"m4 13.03 8-5-8-5v10Z\" class=\"stroke-linejoin-round filled\"/></svg>","stop":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><circle cx=\"8\" cy=\"8\" r=\"7\"/><rect x=\"5\" y=\"5\" width=\"6\" height=\"6\" rx=\".5\" class=\"filled no-stroke\"/></svg>","pause":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M5 2v12M11 2v12\" class=\"stroke-linejoin-round\"/></svg>","trash":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M1 5h14\" class=\"stroke-linejoin-round awsui-icon-lid\"/><path d=\"m13 5-1 10H4L3 5\" class=\"stroke-linejoin-round awsui-icon-side\"/><path d=\"M5 5V2h6v3\" class=\"stroke-linejoin-round awsui-icon-lid\"/></svg>","search":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"m11 11 4 4M7 12A5 5 0 1 0 7 2a5 5 0 0 0 0 10Z\" class=\"stroke-linejoin-round\"/></svg>","archive":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M13 15H3V1h6l4 4v10Z\" class=\"stroke-linejoin-round\"/><path d=\"M8 1v5h5\" class=\"stroke-linejoin-round\"/></svg>","restore":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M5 2 1 6l4 4\" class=\"stroke-linejoin-round awsui-icon-arrow\"/><path d=\"M1 6h9.5c2.49 0 4.5 2.01 4.5 4.5S12.99 15 10.5 15H8\" pathLength=\"1\" class=\"stroke-linejoin-round awsui-icon-line\"/></svg>","refresh":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M15 0v5l-5-.04\" class=\"stroke-linejoin-round\"/><path d=\"M15 8c0 3.87-3.13 7-7 7s-7-3.13-7-7 3.13-7 7-7c2.79 0 5.2 1.63 6.33 4\"/></svg>","link":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M5 8h6\" class=\"stroke-linejoin-round\"/><path d=\"M7 12H5c-2.21 0-4-1.79-4-4s1.79-4 4-4h2\" class=\"stroke-linejoin-round awsui-icon-left\"/><path d=\"M9 12h2c2.21 0 4-1.79 4-4s-1.79-4-4-4H9\" class=\"stroke-linejoin-round awsui-icon-right\"/></svg>","check":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><circle cx=\"8\" cy=\"8\" r=\"7\"/><path d=\"M4.5 7.5 7 10l4-5\" class=\"stroke-linejoin-round\"/></svg>","close":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"m2 1.71 12 12M2 13.71l12-12\" class=\"stroke-linejoin-round\"/></svg>","warn":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M8 5v4M8 10v2M6.52 1.88l-5.33 9.76c-.13.23-.19.5-.19.76 0 .88.71 1.59 1.59 1.59H13.4c.88 0 1.59-.71 1.59-1.59 0-.27-.07-.53-.19-.76L9.48 1.88C9.18 1.34 8.62 1 8 1s-1.18.34-1.48.88Z\" class=\"stroke-linejoin-round\"/></svg>","plus":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M2.01 8h12M8 14l.01-12\"/></svg>","sync":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"m7 7-3 3-3-3M4 9V1\" class=\"stroke-linejoin-round awsui-icon-down\"/><path d=\"m15 9-3-3-3 3M12 7v8\" class=\"stroke-linejoin-round awsui-icon-up\"/><path d=\"M0 14h8M8 2h8\" class=\"stroke-linejoin-round\"/></svg>","copy":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M15 5H5v10h10V5Z\" class=\"stroke-linejoin-round awsui-icon-front\"/><path d=\"M13 1H1v11\" class=\"stroke-linejoin-round awsui-icon-back\"/></svg>","back":"<svg class=\"fn-svg-icon fn-svg-cloudscape\" viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M7 3 2 8l5 5M2 8h13\" class=\"stroke-linejoin-round awsui-icon-glyph\"/></svg>"},"android":{"script":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M560-110v-81q0-5.57 2-10.78 2-5.22 7-10.22l211.61-210.77q9.11-9.12 20.25-13.18Q812-440 823-440q12 0 23 4.5t20 13.5l37 37q9 9 13 20t4 22q0 11-4.5 22.5t-13.58 20.62L692-89q-5 5-10.22 7-5.21 2-10.78 2h-81q-12.75 0-21.37-8.63Q560-97.25 560-110Zm300-233-37-37 37 37ZM620-140h38l121-122-37-37-122 121v38ZM220-80q-24 0-42-18t-18-42v-680q0-24 18-42t42-18h315q12.44 0 23.72 5T578-862l204 204q8 8 13 19.28 5 11.28 5 23.72v71q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63-8.5-8.62-8.5-21.37v-56H550q-12.75 0-21.37-8.63Q520-617.25 520-630v-190H220v680h250q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32Q482.75-80 470-80H220Zm0-60v-680 680Zm541-141-19-18 37 37-18-19Z\"/></svg>","signal":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M140-480q0 63 21 117.5t58 99.5q8 10 8.5 22.5T219-219q-9 9-22.5 8.5T174-222q-45-52-69.5-117.5T80-480q0-75 24.5-140.5T174-738q9-11 22.5-11.5T219-741q9 9 8.5 21.5T219-697q-37 45-58 99.5T140-480Zm160 0q0 29 8.5 55.5T333-376q8 11 7.5 23.5T331-331q-9 9-21 8.5T290-333q-23-30-36.5-67.5T240-480q0-42 13.5-79.5T290-627q8-10 20.5-10.5T332-629q9 9 9.5 21.5T334-584q-16 22-25 48.5t-9 55.5Zm123.5 56.5Q400-447 400-480t23.5-56.5Q447-560 480-560t56.5 23.5Q560-513 560-480t-23.5 56.5Q513-400 480-400t-56.5-23.5ZM660-480q0-29-8.5-55.5T627-584q-8-11-7.5-23.5T629-629q9-9 21-8.5t20 10.5q23 30 36.5 67.5T720-480q0 42-13.5 78.5T670-334q-8 10-20.5 10.5T628-332q-9-9-9.5-21t7.5-23q16-22 25-48.5t9-55.5Zm160 0q0-63-21-117.5T741-697q-8-10-8.5-23t8.5-22q9-9 23-8t22 12q45 53 69.5 118.5T880-480q0 74-24.5 139.5T786-222q-8 11-22 11.5t-23-8.5q-9-9-8.5-21.5T741-263q37-45 58-99.5T820-480Z\"/></svg>","settings":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M421-80q-14 0-25-9t-13-23l-15-94q-19-7-40-19t-37-25l-86 40q-14 6-28 1.5T155-226L97-330q-8-13-4.5-27t15.5-23l80-59q-2-9-2.5-20.5T185-480q0-9 .5-20.5T188-521l-80-59q-12-9-15.5-23t4.5-27l58-104q8-13 22-17.5t28 1.5l86 40q16-13 37-25t40-18l15-95q2-14 13-23t25-9h118q14 0 25 9t13 23l15 94q19 7 40.5 18.5T669-710l86-40q14-6 27.5-1.5T804-734l59 104q8 13 4.5 27.5T852-580l-80 57q2 10 2.5 21.5t.5 21.5q0 10-.5 21t-2.5 21l80 58q12 8 15.5 22.5T863-330l-58 104q-8 13-22 17.5t-28-1.5l-86-40q-16 13-36.5 25.5T592-206l-15 94q-2 14-13 23t-25 9H421Zm15-60h88l14-112q33-8 62.5-25t53.5-41l106 46 40-72-94-69q4-17 6.5-33.5T715-480q0-17-2-33.5t-7-33.5l94-69-40-72-106 46q-23-26-52-43.5T538-708l-14-112h-88l-14 112q-34 7-63.5 24T306-642l-106-46-40 72 94 69q-4 17-6.5 33.5T245-480q0 17 2.5 33.5T254-413l-94 69 40 72 106-46q24 24 53.5 41t62.5 25l14 112Zm44-210q54 0 92-38t38-92q0-54-38-92t-92-38q-54 0-92 38t-38 92q0 54 38 92t92 38Zm0-130Z\"/></svg>","plug":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M190-120q-13 0-21.5-8.5T160-150v-50h-10q-13 0-21.5-8.5T120-230v-120q0-13 8.5-21.5T150-380h50v-310q0-64 47-107t113-43q63 0 106.5 43.5T510-690v420q0 38 26 64t64 26q41 0 70.5-25.5T700-270v-310h-50q-13 0-21.5-8.5T620-610v-120q0-13 8.5-21.5T650-760h10v-50q0-13 8.5-21.5T690-840h80q13 0 21.5 8.5T800-810v50h10q13 0 21.5 8.5T840-730v120q0 13-8.5 21.5T810-580h-50v310q0 64-47 107t-113 43q-63 0-106.5-43.5T450-270v-420q0-38-26-64t-64-26q-41 0-70.5 25.5T260-690v310h50q13 0 21.5 8.5T340-350v120q0 13-8.5 21.5T310-200h-10v50q0 13-8.5 21.5T270-120h-80Z\"/></svg>","lock":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M220-80q-24.75 0-42.37-17.63Q160-115.25 160-140v-434q0-24.75 17.63-42.38Q195.25-634 220-634h70v-96q0-78.85 55.61-134.42Q401.21-920 480.11-920q78.89 0 134.39 55.58Q670-808.85 670-730v96h70q24.75 0 42.38 17.62Q800-598.75 800-574v434q0 24.75-17.62 42.37Q764.75-80 740-80H220Zm0-60h520v-434H220v434Zm314.5-162.03Q557-324.06 557-355q0-30-22.67-54.5t-54.5-24.5q-31.83 0-54.33 24.5t-22.5 55q0 30.5 22.67 52.5t54.5 22q31.83 0 54.33-22.03ZM350-634h260v-96q0-54.17-37.88-92.08-37.88-37.92-92-37.92T388-822.08q-38 37.91-38 92.08v96ZM220-140v-434 434Z\"/></svg>","log":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M222-80q-43.75 0-74.37-30.63Q117-141.25 117-185v-95q0-12.75 8.63-21.38Q134.25-310 147-310h97v-552q0-5.25 4.5-7.13Q253-871 257-867l36 36q4.64 5 10.82 5 6.18 0 11.18-5l38-38q4.64-5 10.82-5 6.18 0 11.18 5l37 38q4.64 5 10.82 5 6.18 0 11.18-5l38-38q4.64-5 10.82-5 6.18 0 11.18 5l38 38q4.64 5 10.82 5 6.18 0 11.18-5l38-38q4.64-5 10.82-5 6.18 0 11.18 5l38 38q4.64 5 10.82 5 6.18 0 11.18-5l38-38q4.64-5 10.82-5 6.18 0 11.18 5l38 38q4.64 5 10.82 5 6.18 0 11.18-5l36-36q4-4 8.5-2.13 4.5 1.88 4.5 7.13v677q0 43.75-30.62 74.37Q781.75-80 738-80H222Zm516-60q20 0 32.5-12.5T783-185v-595H304v470h359q12.75 0 21.38 8.62Q693-292.75 693-280v94.85q0 20.15 12.5 32.65T738-140ZM387-682h180q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H387q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5Zm0 134h180q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H387q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5Zm303-74q-12 0-21-9t-9-21q0-12 9-21t21-9q12 0 21 9t9 21q0 12-9 21t-21 9Zm0 129q-12 0-21-9t-9-21q0-12 9-21t21-9q12 0 21 9t9 21q0 12-9 21t-21 9ZM221-140h412v-110H177v65q0 20 12.65 32.5T221-140Zm-44 0v-110 110Z\"/></svg>","robot":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M147-376q-45 0-76-31.21T40-483q0-44.58 31.21-75.79Q102.42-590 147-590v-123q0-24 18-42t42-18h166q0-45 31-76t76-31q45 0 76 31.21T587-773h166q24 0 42 18t18 42v123q45 0 76 31.21T920-483q0 44.58-31.21 75.79Q857.58-376 813-376v196q0 24-18 42t-42 18H207q-24 0-42-18t-18-42v-196Zm224.5-111.74q11.5-11.73 11.5-28.5 0-16.76-11.74-28.26-11.73-11.5-28.5-11.5-16.76 0-28.26 11.74-11.5 11.73-11.5 28.5 0 16.76 11.74 28.26 11.73 11.5 28.5 11.5 16.76 0 28.26-11.74Zm274 0q11.5-11.73 11.5-28.5 0-16.76-11.74-28.26-11.73-11.5-28.5-11.5-16.76 0-28.26 11.74-11.5 11.73-11.5 28.5 0 16.76 11.74 28.26 11.73 11.5 28.5 11.5 16.76 0 28.26-11.74ZM342-285h276q12.75 0 21.38-8.68 8.62-8.67 8.62-21.5 0-12.82-8.62-21.32-8.63-8.5-21.38-8.5H342q-12.75 0-21.37 8.68-8.63 8.67-8.63 21.5 0 12.82 8.63 21.32 8.62 8.5 21.37 8.5ZM207-180h546v-533H207v533Zm273-267Z\"/></svg>","home":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M220-180h150v-220q0-12.75 8.63-21.38Q387.25-430 400-430h160q12.75 0 21.38 8.62Q590-412.75 590-400v220h150v-390L480-765 220-570v390Zm-60 0v-390q0-14.25 6.38-27 6.37-12.75 17.62-21l260-195q15.68-12 35.84-12Q500-825 516-813l260 195q11.25 8.25 17.63 21 6.37 12.75 6.37 27v390q0 24.75-17.62 42.37Q764.75-120 740-120H560q-12.75 0-21.37-8.63Q530-137.25 530-150v-220H430v220q0 12.75-8.62 21.37Q412.75-120 400-120H220q-24.75 0-42.37-17.63Q160-155.25 160-180Zm320-293Z\"/></svg>","folder":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M140-160q-23 0-41.5-18.5T80-220v-520q0-23 18.5-41.5T140-800h256q12.44 0 23.72 5t19.37 13.09L481-740h369q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H455l-60-60H140v520l90-355q5-20 21.83-32.5Q268.65-620 289-620h574q29 0 47.5 23t10.5 52l-88 339q-6 24-22 35t-41 11H140Zm63-60h572l84-340H287l-84 340Zm-63-353v-167 167Zm63 353 84-340-84 340Z\"/></svg>","server":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M286.88-717q-20.88 0-35.38 14.62-14.5 14.62-14.5 35.5 0 20.88 14.62 35.38 14.62 14.5 35.5 14.5 20.88 0 35.38-14.62 14.5-14.62 14.5-35.5 0-20.88-14.62-35.38-14.62-14.5-35.5-14.5Zm0 414q-20.88 0-35.38 14.62-14.5 14.62-14.5 35.5 0 20.88 14.62 35.38 14.62 14.5 35.5 14.5 20.88 0 35.38-14.62 14.5-14.62 14.5-35.5 0-20.88-14.62-35.38-14.62-14.5-35.5-14.5ZM154-839h651q16 0 25.5 9.5t9.5 25.81V-535q0 17.42-9.5 29.21T805-494H154q-15 0-24.5-11.79T120-535v-268.69q0-16.31 9.5-25.81T154-839Zm26 60v225h600v-225H180Zm-26 353h647q15 0 27 12.5t12 28.53V-121q0 20-12 30.5T801-80H159q-16 0-27.5-10.5T120-121v-263.97q0-16.03 9.5-28.53T154-426Zm26 60v226h600v-226H180Zm0-413v225-225Zm0 413v226-226Z\"/></svg>","save":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M180-120q-24 0-42-18t-18-42v-600q0-24 18-42t42-18h478q12.44 0 23.72 5T701-822l121 121q8 8 13 19.28 5 11.28 5 23.72v478q0 24-18 42t-42 18H180Zm600-536L656-780H180v600h600v-476ZM553.5-275.26q30.5-30.27 30.5-73.5 0-43.24-30.26-73.74-30.27-30.5-73.5-30.5-43.24 0-73.74 30.26-30.5 30.27-30.5 73.5 0 43.24 30.26 73.74 30.27 30.5 73.5 30.5 43.24 0 73.74-30.26ZM263-584h298q12.75 0 21.38-8.63Q591-601.25 591-614v-83q0-12.75-8.62-21.38Q573.75-727 561-727H263q-12.75 0-21.37 8.62Q233-709.75 233-697v83q0 12.75 8.63 21.37Q250.25-584 263-584Zm-83-72v476-600 124Z\"/></svg>","upload":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M220-160q-24 0-42-18t-18-42v-113q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v113h520v-113q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v113q0 24-18 42t-42 18H220Zm230-524-99 99q-8.8 9-20.9 8.5-12.1-.5-21.49-9.5-8.61-9-8.61-21.5t9-21.5l150-150q5-5 10.13-7 5.14-2 11-2 5.87 0 10.87 2 5 2 10 7l151 151q9 9 9 21t-8.61 21q-9.39 9-21.89 9t-21.5-9l-99-98v341q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63-8.5-8.62-8.5-21.37v-341Z\"/></svg>","download":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M469-327q-5-2-10-7L308-485q-9-9.27-8.5-21.64.5-12.36 9.11-21.36 9.39-9 21.89-9t21.5 9l98 99v-341q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v341l99-99q8.8-9 20.9-8.5 12.1.5 21.49 9.5 8.61 9 8.61 21.5t-9 21.5L501-334q-5 5-10.13 7-5.14 2-11 2-5.87 0-10.87-2ZM220-160q-24 0-42-18t-18-42v-113q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v113h520v-113q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v113q0 24-18 42t-42 18H220Z\"/></svg>","play":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M320-258v-450q0-14 9-22t21-8q4 0 8 1t8 3l354 226q7 5 10.5 11t3.5 14q0 8-3.5 14T720-458L366-232q-4 2-8 3t-8 1q-12 0-21-8t-9-22Zm60-225Zm0 171 269-171-269-171v342Z\"/></svg>","stop":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M240-300v-360q0-24.75 17.63-42.38Q275.25-720 300-720h360q24.75 0 42.38 17.62Q720-684.75 720-660v360q0 24.75-17.62 42.37Q684.75-240 660-240H300q-24.75 0-42.37-17.63Q240-275.25 240-300Zm60 0h360v-360H300v360Zm180-180Z\"/></svg>","pause":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M585-200q-24.75 0-42.37-17.63Q525-235.25 525-260v-440q0-24.75 17.63-42.38Q560.25-760 585-760h115q24.75 0 42.38 17.62Q760-724.75 760-700v440q0 24.75-17.62 42.37Q724.75-200 700-200H585Zm-325 0q-24.75 0-42.37-17.63Q200-235.25 200-260v-440q0-24.75 17.63-42.38Q235.25-760 260-760h115q24.75 0 42.38 17.62Q435-724.75 435-700v440q0 24.75-17.62 42.37Q399.75-200 375-200H260Zm325-60h115v-440H585v440Zm-325 0h115v-440H260v440Zm0-440v440-440Zm325 0v440-440Z\"/></svg>","trash":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M261-120q-24.75 0-42.37-17.63Q201-155.25 201-180v-570h-11q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h158q0-13 8.63-21.5 8.62-8.5 21.37-8.5h204q12.75 0 21.38 8.62Q612-822.75 612-810h158q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5h-11v570q0 24.75-17.62 42.37Q723.75-120 699-120H261Zm438-630H261v570h438v-570ZM418.5-274.63q8.5-8.62 8.5-21.37v-339q0-12.75-8.68-21.38-8.67-8.62-21.5-8.62-12.82 0-21.32 8.62-8.5 8.63-8.5 21.38v339q0 12.75 8.68 21.37 8.67 8.63 21.5 8.63 12.82 0 21.32-8.63Zm166 0q8.5-8.62 8.5-21.37v-339q0-12.75-8.68-21.38-8.67-8.62-21.5-8.62-12.82 0-21.32 8.62-8.5 8.63-8.5 21.38v339q0 12.75 8.68 21.37 8.67 8.63 21.5 8.63 12.82 0 21.32-8.63ZM261-750v570-570Z\"/></svg>","search":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M378-329q-108.16 0-183.08-75Q120-479 120-585t75-181q75-75 181.5-75t181 75Q632-691 632-584.85 632-542 618-502q-14 40-42 75l242 240q9 8.56 9 21.78T818-143q-9 9-22.22 9-13.22 0-21.78-9L533-384q-30 26-69.96 40.5Q423.08-329 378-329Zm-1-60q81.25 0 138.13-57.5Q572-504 572-585t-56.87-138.5Q458.25-781 377-781q-82.08 0-139.54 57.5Q180-666 180-585t57.46 138.5Q294.92-389 377-389Z\"/></svg>","archive":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M180-80q-24.75 0-42.37-17.63Q120-115.25 120-140v-483q-17-6-28.5-21.39T80-680v-140q0-24.75 17.63-42.38Q115.25-880 140-880h680q24.75 0 42.38 17.62Q880-844.75 880-820v140q0 20.22-11.5 35.61T840-623v483q0 24.75-17.62 42.37Q804.75-80 780-80H180Zm0-540v480h600v-480H180Zm-40-60h680v-140H140v140Zm250 260h180q12.75 0 21.38-8.68 8.62-8.67 8.62-21.5 0-12.82-8.62-21.32-8.63-8.5-21.38-8.5H390q-12.75 0-21.37 8.68-8.63 8.67-8.63 21.5 0 12.82 8.63 21.32 8.62 8.5 21.37 8.5Zm90 40Z\"/></svg>","restore":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M480-120q-143 0-245.5-95.5T121-451q-1-12 7.5-21t21.38-9q12.12 0 20.62 8.5Q179-464 181-451q11 115 96 193t203 78q125.36 0 212.68-87.32Q780-354.64 780-480q0-125.36-87.32-212.68Q605.36-780 480-780q-69 0-129 30.5T246-667h74q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H172q-12.75 0-21.37-8.63Q142-624.25 142-637v-148q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v76q53-62 125.22-96.5Q399.43-840 480-840q75 0 140.5 28.5t114 77q48.5 48.5 77 114T840-480q0 75-28.5 140.5t-77 114q-48.5 48.5-114 77T480-120Zm-.04-285Q449-405 427-427.04q-22-22.05-22-53Q405-511 427.04-533q22.05-22 53-22Q511-555 533-532.96q22 22.05 22 53Q555-449 532.96-427q-22.05 22-53 22Z\"/></svg>","refresh":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M480-160q-133 0-226.5-93.5T160-480q0-133 93.5-226.5T480-800q85 0 149 34.5T740-671v-99q0-13 8.5-21.5T770-800q13 0 21.5 8.5T800-770v194q0 13-8.5 21.5T770-546H576q-13 0-21.5-8.5T546-576q0-13 8.5-21.5T576-606h138q-38-60-97-97t-137-37q-109 0-184.5 75.5T220-480q0 109 75.5 184.5T480-220q75 0 140-39.5T717-366q5-11 16.5-16.5t22.5-.5q12 5 16 16.5t-1 23.5q-39 84-117.5 133.5T480-160Z\"/></svg>","link":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M280-280q-83 0-141.5-58.5T80-480q0-83 58.5-141.5T280-680h140q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H280q-58.33 0-99.17 40.76-40.83 40.77-40.83 99Q140-422 180.83-381q40.84 41 99.17 41h140q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H280Zm75-170q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h250q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H355Zm185 170q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h140q58.33 0 99.17-40.76 40.83-40.77 40.83-99Q820-538 779.17-579q-40.84-41-99.17-41H540q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h140q83 0 141.5 58.5T880-480q0 83-58.5 141.5T680-280H540Z\"/></svg>","check":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"m378-332 363-363q9-9 21.5-9t21.5 9q9 9 9 21.5t-9 21.5L399-267q-9 9-21 9t-21-9L175-449q-9-9-8.5-21.5T176-492q9-9 21.5-9t21.5 9l159 160Z\"/></svg>","close":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M480-438 270-228q-9 9-21 9t-21-9q-9-9-9-21t9-21l210-210-210-210q-9-9-9-21t9-21q9-9 21-9t21 9l210 210 210-210q9-9 21-9t21 9q9 9 9 21t-9 21L522-480l210 210q9 9 9 21t-9 21q-9 9-21 9t-21-9L480-438Z\"/></svg>","warn":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M92-120q-9 0-15.5-4T66-135q-4-7-4.5-14.5T66-165l388-670q5-8 11.5-11.5T480-850q8 0 14.5 3.5T506-835l388 670q5 8 4.5 15.5T894-135q-4 7-10.5 11t-15.5 4H92Zm52-60h672L480-760 144-180Zm361.5-65.5Q514-254 514-267t-8.5-21.5Q497-297 484-297t-21.5 8.5Q454-280 454-267t8.5 21.5Q471-237 484-237t21.5-8.5Zm0-111Q514-365 514-378v-164q0-13-8.5-21.5T484-572q-13 0-21.5 8.5T454-542v164q0 13 8.5 21.5T484-348q13 0 21.5-8.5ZM480-470Z\"/></svg>","plus":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M450-450H230q-12.75 0-21.37-8.68-8.63-8.67-8.63-21.5 0-12.82 8.63-21.32 8.62-8.5 21.37-8.5h220v-220q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v220h220q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32-8.63 8.5-21.38 8.5H510v220q0 12.75-8.68 21.37-8.67 8.63-21.5 8.63-12.82 0-21.32-8.63-8.5-8.62-8.5-21.37v-220Z\"/></svg>","sync":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"m194-281 98 98q9 9 9 21t-9 21q-9 9-21 9t-21-9L101-290q-5-5-7-10t-2-11q0-6 2-11t7-10l150-150q9-9 21-9t21 9q9 9 9 21t-9 21l-99 99h616q13 0 21.5 8.5T840-311q0 13-8.5 21.5T810-281H194Zm572-337H150q-13 0-21.5-8.5T120-648q0-13 8.5-21.5T150-678h616l-99-99q-9-9-9-21t9-21q9-9 21-9t21 9l150 150q5 5 7 10t2 11q0 6-2 11t-7 10L710-478q-9 9-21 9t-21-9q-9-9-9-21t9-21l98-98Z\"/></svg>","copy":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"M300-200q-24 0-42-18t-18-42v-560q0-24 18-42t42-18h440q24 0 42 18t18 42v560q0 24-18 42t-42 18H300Zm0-60h440v-560H300v560ZM180-80q-24 0-42-18t-18-42v-590q0-12.75 8.68-21.38 8.67-8.62 21.5-8.62 12.82 0 21.32 8.62 8.5 8.63 8.5 21.38v590h470q12.75 0 21.38 8.68 8.62 8.67 8.62 21.5 0 12.82-8.62 21.32Q662.75-80 650-80H180Zm120-180v-560 560Z\"/></svg>","back":"<svg class=\"fn-svg-icon fn-svg-material\" aria-hidden=\"true\" viewBox=\"0 -960 960 960\" fill=\"currentColor\"><path d=\"m274-450 227 227q9 9 9 21t-9 21q-9 9-21 9t-21-9L181-459q-5-5-7-10t-2-11q0-6 2-11t7-10l278-278q9-9 21-9t21 9q9 9 9 21t-9 21L274-510h496q13 0 21.5 8.5T800-480q0 13-8.5 21.5T770-450H274Z\"/></svg>"},"lucide-clean":{"script":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z\" /> <path d=\"M14 2v5a1 1 0 0 0 1 1h5\" /> <path d=\"M10 12.5 8 15l2 2.5\" /> <path d=\"m14 12.5 2 2.5-2 2.5\" /></svg>","signal":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M16.247 7.761a6 6 0 0 1 0 8.478\" /> <path d=\"M19.075 4.933a10 10 0 0 1 0 14.134\" /> <path d=\"M4.925 19.067a10 10 0 0 1 0-14.134\" /> <path d=\"M7.753 16.239a6 6 0 0 1 0-8.478\" /> <circle cx=\"12\" cy=\"12\" r=\"2\" /></svg>","settings":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M9.671 4.136a2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1-2.33 4.033 2.34 2.34 0 0 0-3.319 1.915 2.34 2.34 0 0 1-4.659 0 2.34 2.34 0 0 0-3.32-1.915 2.34 2.34 0 0 1-2.33-4.033 2.34 2.34 0 0 0 0-3.831A2.34 2.34 0 0 1 6.35 6.051a2.34 2.34 0 0 0 3.319-1.915\" /> <circle cx=\"12\" cy=\"12\" r=\"3\" /></svg>","plug":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 22v-5\" /> <path d=\"M15 8V2\" /> <path d=\"M17 8a1 1 0 0 1 1 1v4a4 4 0 0 1-4 4h-4a4 4 0 0 1-4-4V9a1 1 0 0 1 1-1z\" /> <path d=\"M9 8V2\" /></svg>","lock":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect width=\"18\" height=\"11\" x=\"3\" y=\"11\" rx=\"2\" ry=\"2\" /> <path d=\"M7 11V7a5 5 0 0 1 10 0v4\" /></svg>","log":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M15 12h-5\" /> <path d=\"M15 8h-5\" /> <path d=\"M19 17V5a2 2 0 0 0-2-2H4\" /> <path d=\"M8 21h12a2 2 0 0 0 2-2v-1a1 1 0 0 0-1-1H11a1 1 0 0 0-1 1v1a2 2 0 1 1-4 0V5a2 2 0 1 0-4 0v2a1 1 0 0 0 1 1h3\" /></svg>","robot":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 8V4H8\" /> <rect width=\"16\" height=\"12\" x=\"4\" y=\"8\" rx=\"2\" /> <path d=\"M2 14h2\" /> <path d=\"M20 14h2\" /> <path d=\"M15 13v2\" /> <path d=\"M9 13v2\" /></svg>","home":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8\" /> <path d=\"M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z\" /></svg>","folder":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"m6 14 1.5-2.9A2 2 0 0 1 9.24 10H20a2 2 0 0 1 1.94 2.5l-1.54 6a2 2 0 0 1-1.95 1.5H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h3.9a2 2 0 0 1 1.69.9l.81 1.2a2 2 0 0 0 1.67.9H18a2 2 0 0 1 2 2v2\" /></svg>","server":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect width=\"20\" height=\"8\" x=\"2\" y=\"2\" rx=\"2\" ry=\"2\" /> <rect width=\"20\" height=\"8\" x=\"2\" y=\"14\" rx=\"2\" ry=\"2\" /> <line x1=\"6\" x2=\"6.01\" y1=\"6\" y2=\"6\" /> <line x1=\"6\" x2=\"6.01\" y1=\"18\" y2=\"18\" /></svg>","save":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M15.2 3a2 2 0 0 1 1.4.6l3.8 3.8a2 2 0 0 1 .6 1.4V19a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z\" /> <path d=\"M17 21v-7a1 1 0 0 0-1-1H8a1 1 0 0 0-1 1v7\" /> <path d=\"M7 3v4a1 1 0 0 0 1 1h7\" /></svg>","upload":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 3v12\" /> <path d=\"m17 8-5-5-5 5\" /> <path d=\"M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4\" /></svg>","download":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M12 15V3\" /> <path d=\"M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4\" /> <path d=\"m7 10 5 5 5-5\" /></svg>","play":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M5 5a2 2 0 0 1 3.008-1.728l11.997 6.998a2 2 0 0 1 .003 3.458l-12 7A2 2 0 0 1 5 19z\" /></svg>","stop":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect width=\"18\" height=\"18\" x=\"3\" y=\"3\" rx=\"2\" /></svg>","pause":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect x=\"14\" y=\"3\" width=\"5\" height=\"18\" rx=\"1\" /> <rect x=\"5\" y=\"3\" width=\"5\" height=\"18\" rx=\"1\" /></svg>","trash":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M10 11v6\" /> <path d=\"M14 11v6\" /> <path d=\"M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6\" /> <path d=\"M3 6h18\" /> <path d=\"M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2\" /></svg>","search":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"m21 21-4.34-4.34\" /> <circle cx=\"11\" cy=\"11\" r=\"8\" /></svg>","archive":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect width=\"20\" height=\"5\" x=\"2\" y=\"3\" rx=\"1\" /> <path d=\"M4 8v11a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8\" /> <path d=\"M10 12h4\" /></svg>","restore":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8\" /> <path d=\"M3 3v5h5\" /></svg>","refresh":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8\" /> <path d=\"M21 3v5h-5\" /> <path d=\"M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16\" /> <path d=\"M8 16H3v5\" /></svg>","link":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71\" /> <path d=\"M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71\" /></svg>","check":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M20 6 9 17l-5-5\" /></svg>","close":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M18 6 6 18\" /> <path d=\"m6 6 12 12\" /></svg>","warn":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3\" /> <path d=\"M12 9v4\" /> <path d=\"M12 17h.01\" /></svg>","plus":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M5 12h14\" /> <path d=\"M12 5v14\" /></svg>","sync":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"M8 3 4 7l4 4\" /> <path d=\"M4 7h16\" /> <path d=\"m16 21 4-4-4-4\" /> <path d=\"M20 17H4\" /></svg>","copy":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><rect width=\"14\" height=\"14\" x=\"8\" y=\"8\" rx=\"2\" ry=\"2\" /> <path d=\"M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2\" /></svg>","back":"<svg class=\"fn-svg-icon fn-svg-lucide\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke- stroke-linecap=\"round\" stroke-linejoin=\"round\"><path d=\"m12 19-7-7 7-7\" /> <path d=\"M19 12H5\" /></svg>"},"hero-solid":{"script":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0 0 16.5 9h-1.875a1.875 1.875 0 0 1-1.875-1.875V5.25A3.75 3.75 0 0 0 9 1.5H5.625ZM7.5 15a.75.75 0 0 1 .75-.75h7.5a.75.75 0 0 1 0 1.5h-7.5A.75.75 0 0 1 7.5 15Zm.75 2.25a.75.75 0 0 0 0 1.5H12a.75.75 0 0 0 0-1.5H8.25Z\" clip-rule=\"evenodd\"/> <path d=\"M12.971 1.816A5.23 5.23 0 0 1 14.25 5.25v1.875c0 .207.168.375.375.375H16.5a5.23 5.23 0 0 1 3.434 1.279 9.768 9.768 0 0 0-6.963-6.963Z\"/></svg>","signal":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M5.636 4.575a.75.75 0 0 1 0 1.061 9 9 0 0 0 0 12.728.75.75 0 1 1-1.06 1.06c-4.101-4.1-4.101-10.748 0-14.849a.75.75 0 0 1 1.06 0Zm12.728 0a.75.75 0 0 1 1.06 0c4.101 4.1 4.101 10.75 0 14.85a.75.75 0 1 1-1.06-1.061 9 9 0 0 0 0-12.728.75.75 0 0 1 0-1.06ZM7.757 6.697a.75.75 0 0 1 0 1.06 6 6 0 0 0 0 8.486.75.75 0 0 1-1.06 1.06 7.5 7.5 0 0 1 0-10.606.75.75 0 0 1 1.06 0Zm8.486 0a.75.75 0 0 1 1.06 0 7.5 7.5 0 0 1 0 10.606.75.75 0 0 1-1.06-1.06 6 6 0 0 0 0-8.486.75.75 0 0 1 0-1.06ZM9.879 8.818a.75.75 0 0 1 0 1.06 3 3 0 0 0 0 4.243.75.75 0 1 1-1.061 1.061 4.5 4.5 0 0 1 0-6.364.75.75 0 0 1 1.06 0Zm4.242 0a.75.75 0 0 1 1.061 0 4.5 4.5 0 0 1 0 6.364.75.75 0 0 1-1.06-1.06 3 3 0 0 0 0-4.243.75.75 0 0 1 0-1.061ZM10.875 12a1.125 1.125 0 1 1 2.25 0 1.125 1.125 0 0 1-2.25 0Z\" clip-rule=\"evenodd\"/></svg>","settings":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M11.078 2.25c-.917 0-1.699.663-1.85 1.567L9.05 4.889c-.02.12-.115.26-.297.348a7.493 7.493 0 0 0-.986.57c-.166.115-.334.126-.45.083L6.3 5.508a1.875 1.875 0 0 0-2.282.819l-.922 1.597a1.875 1.875 0 0 0 .432 2.385l.84.692c.095.078.17.229.154.43a7.598 7.598 0 0 0 0 1.139c.015.2-.059.352-.153.43l-.841.692a1.875 1.875 0 0 0-.432 2.385l.922 1.597a1.875 1.875 0 0 0 2.282.818l1.019-.382c.115-.043.283-.031.45.082.312.214.641.405.985.57.182.088.277.228.297.35l.178 1.071c.151.904.933 1.567 1.85 1.567h1.844c.916 0 1.699-.663 1.85-1.567l.178-1.072c.02-.12.114-.26.297-.349.344-.165.673-.356.985-.57.167-.114.335-.125.45-.082l1.02.382a1.875 1.875 0 0 0 2.28-.819l.923-1.597a1.875 1.875 0 0 0-.432-2.385l-.84-.692c-.095-.078-.17-.229-.154-.43a7.614 7.614 0 0 0 0-1.139c-.016-.2.059-.352.153-.43l.84-.692c.708-.582.891-1.59.433-2.385l-.922-1.597a1.875 1.875 0 0 0-2.282-.818l-1.02.382c-.114.043-.282.031-.449-.083a7.49 7.49 0 0 0-.985-.57c-.183-.087-.277-.227-.297-.348l-.179-1.072a1.875 1.875 0 0 0-1.85-1.567h-1.843ZM12 15.75a3.75 3.75 0 1 0 0-7.5 3.75 3.75 0 0 0 0 7.5Z\" clip-rule=\"evenodd\"/></svg>","plug":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M14.615 1.595a.75.75 0 0 1 .359.852L12.982 9.75h7.268a.75.75 0 0 1 .548 1.262l-10.5 11.25a.75.75 0 0 1-1.272-.71l1.992-7.302H3.75a.75.75 0 0 1-.548-1.262l10.5-11.25a.75.75 0 0 1 .913-.143Z\" clip-rule=\"evenodd\"/></svg>","lock":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M12 1.5a5.25 5.25 0 0 0-5.25 5.25v3a3 3 0 0 0-3 3v6.75a3 3 0 0 0 3 3h10.5a3 3 0 0 0 3-3v-6.75a3 3 0 0 0-3-3v-3c0-2.9-2.35-5.25-5.25-5.25Zm3.75 8.25v-3a3.75 3.75 0 1 0-7.5 0v3h7.5Z\" clip-rule=\"evenodd\"/></svg>","log":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M5.625 3.75a2.625 2.625 0 1 0 0 5.25h12.75a2.625 2.625 0 0 0 0-5.25H5.625ZM3.75 11.25a.75.75 0 0 0 0 1.5h16.5a.75.75 0 0 0 0-1.5H3.75ZM3 15.75a.75.75 0 0 1 .75-.75h16.5a.75.75 0 0 1 0 1.5H3.75a.75.75 0 0 1-.75-.75ZM3.75 18.75a.75.75 0 0 0 0 1.5h16.5a.75.75 0 0 0 0-1.5H3.75Z\"/></svg>","robot":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M16.5 7.5h-9v9h9v-9Z\"/> <path fill-rule=\"evenodd\" d=\"M8.25 2.25A.75.75 0 0 1 9 3v.75h2.25V3a.75.75 0 0 1 1.5 0v.75H15V3a.75.75 0 0 1 1.5 0v.75h.75a3 3 0 0 1 3 3v.75H21A.75.75 0 0 1 21 9h-.75v2.25H21a.75.75 0 0 1 0 1.5h-.75V15H21a.75.75 0 0 1 0 1.5h-.75v.75a3 3 0 0 1-3 3h-.75V21a.75.75 0 0 1-1.5 0v-.75h-2.25V21a.75.75 0 0 1-1.5 0v-.75H9V21a.75.75 0 0 1-1.5 0v-.75h-.75a3 3 0 0 1-3-3v-.75H3A.75.75 0 0 1 3 15h.75v-2.25H3a.75.75 0 0 1 0-1.5h.75V9H3a.75.75 0 0 1 0-1.5h.75v-.75a3 3 0 0 1 3-3h.75V3a.75.75 0 0 1 .75-.75ZM6 6.75A.75.75 0 0 1 6.75 6h10.5a.75.75 0 0 1 .75.75v10.5a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V6.75Z\" clip-rule=\"evenodd\"/></svg>","home":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M11.47 3.841a.75.75 0 0 1 1.06 0l8.69 8.69a.75.75 0 1 0 1.06-1.061l-8.689-8.69a2.25 2.25 0 0 0-3.182 0l-8.69 8.69a.75.75 0 1 0 1.061 1.06l8.69-8.689Z\"/> <path d=\"m12 5.432 8.159 8.159c.03.03.06.058.091.086v6.198c0 1.035-.84 1.875-1.875 1.875H15a.75.75 0 0 1-.75-.75v-4.5a.75.75 0 0 0-.75-.75h-3a.75.75 0 0 0-.75.75V21a.75.75 0 0 1-.75.75H5.625a1.875 1.875 0 0 1-1.875-1.875v-6.198a2.29 2.29 0 0 0 .091-.086L12 5.432Z\"/></svg>","folder":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M19.906 9c.382 0 .749.057 1.094.162V9a3 3 0 0 0-3-3h-3.879a.75.75 0 0 1-.53-.22L11.47 3.66A2.25 2.25 0 0 0 9.879 3H6a3 3 0 0 0-3 3v3.162A3.756 3.756 0 0 1 4.094 9h15.812ZM4.094 10.5a2.25 2.25 0 0 0-2.227 2.568l.857 6A2.25 2.25 0 0 0 4.951 21H19.05a2.25 2.25 0 0 0 2.227-1.932l.857-6a2.25 2.25 0 0 0-2.227-2.568H4.094Z\"/></svg>","server":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M5.507 4.048A3 3 0 0 1 7.785 3h8.43a3 3 0 0 1 2.278 1.048l1.722 2.008A4.533 4.533 0 0 0 19.5 6h-15c-.243 0-.482.02-.715.056l1.722-2.008Z\"/> <path fill-rule=\"evenodd\" d=\"M1.5 10.5a3 3 0 0 1 3-3h15a3 3 0 1 1 0 6h-15a3 3 0 0 1-3-3Zm15 0a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Zm2.25.75a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5ZM4.5 15a3 3 0 1 0 0 6h15a3 3 0 1 0 0-6h-15Zm11.25 3.75a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5ZM19.5 18a.75.75 0 1 1-1.5 0 .75.75 0 0 1 1.5 0Z\" clip-rule=\"evenodd\"/></svg>","save":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M12 1.5a.75.75 0 0 1 .75.75V7.5h-1.5V2.25A.75.75 0 0 1 12 1.5ZM11.25 7.5v5.69l-1.72-1.72a.75.75 0 0 0-1.06 1.06l3 3a.75.75 0 0 0 1.06 0l3-3a.75.75 0 1 0-1.06-1.06l-1.72 1.72V7.5h3.75a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3h-9a3 3 0 0 1-3-3v-9a3 3 0 0 1 3-3h3.75Z\"/></svg>","upload":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M11.47 2.47a.75.75 0 0 1 1.06 0l4.5 4.5a.75.75 0 0 1-1.06 1.06l-3.22-3.22V16.5a.75.75 0 0 1-1.5 0V4.81L8.03 8.03a.75.75 0 0 1-1.06-1.06l4.5-4.5ZM3 15.75a.75.75 0 0 1 .75.75v2.25a1.5 1.5 0 0 0 1.5 1.5h13.5a1.5 1.5 0 0 0 1.5-1.5V16.5a.75.75 0 0 1 1.5 0v2.25a3 3 0 0 1-3 3H5.25a3 3 0 0 1-3-3V16.5a.75.75 0 0 1 .75-.75Z\" clip-rule=\"evenodd\"/></svg>","download":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M12 2.25a.75.75 0 0 1 .75.75v11.69l3.22-3.22a.75.75 0 1 1 1.06 1.06l-4.5 4.5a.75.75 0 0 1-1.06 0l-4.5-4.5a.75.75 0 1 1 1.06-1.06l3.22 3.22V3a.75.75 0 0 1 .75-.75Zm-9 13.5a.75.75 0 0 1 .75.75v2.25a1.5 1.5 0 0 0 1.5 1.5h13.5a1.5 1.5 0 0 0 1.5-1.5V16.5a.75.75 0 0 1 1.5 0v2.25a3 3 0 0 1-3 3H5.25a3 3 0 0 1-3-3V16.5a.75.75 0 0 1 .75-.75Z\" clip-rule=\"evenodd\"/></svg>","play":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M4.5 5.653c0-1.427 1.529-2.33 2.779-1.643l11.54 6.347c1.295.712 1.295 2.573 0 3.286L7.28 19.99c-1.25.687-2.779-.217-2.779-1.643V5.653Z\" clip-rule=\"evenodd\"/></svg>","stop":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M4.5 7.5a3 3 0 0 1 3-3h9a3 3 0 0 1 3 3v9a3 3 0 0 1-3 3h-9a3 3 0 0 1-3-3v-9Z\" clip-rule=\"evenodd\"/></svg>","pause":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M6.75 5.25a.75.75 0 0 1 .75-.75H9a.75.75 0 0 1 .75.75v13.5a.75.75 0 0 1-.75.75H7.5a.75.75 0 0 1-.75-.75V5.25Zm7.5 0A.75.75 0 0 1 15 4.5h1.5a.75.75 0 0 1 .75.75v13.5a.75.75 0 0 1-.75.75H15a.75.75 0 0 1-.75-.75V5.25Z\" clip-rule=\"evenodd\"/></svg>","trash":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M16.5 4.478v.227a48.816 48.816 0 0 1 3.878.512.75.75 0 1 1-.256 1.478l-.209-.035-1.005 13.07a3 3 0 0 1-2.991 2.77H8.084a3 3 0 0 1-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 0 1-.256-1.478A48.567 48.567 0 0 1 7.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 0 1 3.369 0c1.603.051 2.815 1.387 2.815 2.951Zm-6.136-1.452a51.196 51.196 0 0 1 3.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 0 0-6 0v-.113c0-.794.609-1.428 1.364-1.452Zm-.355 5.945a.75.75 0 1 0-1.5.058l.347 9a.75.75 0 1 0 1.499-.058l-.346-9Zm5.48.058a.75.75 0 1 0-1.498-.058l-.347 9a.75.75 0 0 0 1.5.058l.345-9Z\" clip-rule=\"evenodd\"/></svg>","search":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M10.5 3.75a6.75 6.75 0 1 0 0 13.5 6.75 6.75 0 0 0 0-13.5ZM2.25 10.5a8.25 8.25 0 1 1 14.59 5.28l4.69 4.69a.75.75 0 1 1-1.06 1.06l-4.69-4.69A8.25 8.25 0 0 1 2.25 10.5Z\" clip-rule=\"evenodd\"/></svg>","archive":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path d=\"M3.375 3C2.339 3 1.5 3.84 1.5 4.875v.75c0 1.036.84 1.875 1.875 1.875h17.25c1.035 0 1.875-.84 1.875-1.875v-.75C22.5 3.839 21.66 3 20.625 3H3.375Z\"/> <path fill-rule=\"evenodd\" d=\"m3.087 9 .54 9.176A3 3 0 0 0 6.62 21h10.757a3 3 0 0 0 2.995-2.824L20.913 9H3.087Zm6.163 3.75A.75.75 0 0 1 10 12h4a.75.75 0 0 1 0 1.5h-4a.75.75 0 0 1-.75-.75Z\" clip-rule=\"evenodd\"/></svg>","restore":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M9.53 2.47a.75.75 0 0 1 0 1.06L4.81 8.25H15a6.75 6.75 0 0 1 0 13.5h-3a.75.75 0 0 1 0-1.5h3a5.25 5.25 0 1 0 0-10.5H4.81l4.72 4.72a.75.75 0 1 1-1.06 1.06l-6-6a.75.75 0 0 1 0-1.06l6-6a.75.75 0 0 1 1.06 0Z\" clip-rule=\"evenodd\"/></svg>","refresh":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M4.755 10.059a7.5 7.5 0 0 1 12.548-3.364l1.903 1.903h-3.183a.75.75 0 1 0 0 1.5h4.992a.75.75 0 0 0 .75-.75V4.356a.75.75 0 0 0-1.5 0v3.18l-1.9-1.9A9 9 0 0 0 3.306 9.67a.75.75 0 1 0 1.45.388Zm15.408 3.352a.75.75 0 0 0-.919.53 7.5 7.5 0 0 1-12.548 3.364l-1.902-1.903h3.183a.75.75 0 0 0 0-1.5H2.984a.75.75 0 0 0-.75.75v4.992a.75.75 0 0 0 1.5 0v-3.18l1.9 1.9a9 9 0 0 0 15.059-4.035.75.75 0 0 0-.53-.918Z\" clip-rule=\"evenodd\"/></svg>","link":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M19.902 4.098a3.75 3.75 0 0 0-5.304 0l-4.5 4.5a3.75 3.75 0 0 0 1.035 6.037.75.75 0 0 1-.646 1.353 5.25 5.25 0 0 1-1.449-8.45l4.5-4.5a5.25 5.25 0 1 1 7.424 7.424l-1.757 1.757a.75.75 0 1 1-1.06-1.06l1.757-1.757a3.75 3.75 0 0 0 0-5.304Zm-7.389 4.267a.75.75 0 0 1 1-.353 5.25 5.25 0 0 1 1.449 8.45l-4.5 4.5a5.25 5.25 0 1 1-7.424-7.424l1.757-1.757a.75.75 0 1 1 1.06 1.06l-1.757 1.757a3.75 3.75 0 1 0 5.304 5.304l4.5-4.5a3.75 3.75 0 0 0-1.035-6.037.75.75 0 0 1-.354-1Z\" clip-rule=\"evenodd\"/></svg>","check":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M19.916 4.626a.75.75 0 0 1 .208 1.04l-9 13.5a.75.75 0 0 1-1.154.114l-6-6a.75.75 0 0 1 1.06-1.06l5.353 5.353 8.493-12.74a.75.75 0 0 1 1.04-.207Z\" clip-rule=\"evenodd\"/></svg>","close":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M5.47 5.47a.75.75 0 0 1 1.06 0L12 10.94l5.47-5.47a.75.75 0 1 1 1.06 1.06L13.06 12l5.47 5.47a.75.75 0 1 1-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 0 1-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 0 1 0-1.06Z\" clip-rule=\"evenodd\"/></svg>","warn":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M9.401 3.003c1.155-2 4.043-2 5.197 0l7.355 12.748c1.154 2-.29 4.5-2.599 4.5H4.645c-2.309 0-3.752-2.5-2.598-4.5L9.4 3.003ZM12 8.25a.75.75 0 0 1 .75.75v3.75a.75.75 0 0 1-1.5 0V9a.75.75 0 0 1 .75-.75Zm0 8.25a.75.75 0 1 0 0-1.5.75.75 0 0 0 0 1.5Z\" clip-rule=\"evenodd\"/></svg>","plus":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M12 3.75a.75.75 0 0 1 .75.75v6.75h6.75a.75.75 0 0 1 0 1.5h-6.75v6.75a.75.75 0 0 1-1.5 0v-6.75H4.5a.75.75 0 0 1 0-1.5h6.75V4.5a.75.75 0 0 1 .75-.75Z\" clip-rule=\"evenodd\"/></svg>","sync":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M15.97 2.47a.75.75 0 0 1 1.06 0l4.5 4.5a.75.75 0 0 1 0 1.06l-4.5 4.5a.75.75 0 1 1-1.06-1.06l3.22-3.22H7.5a.75.75 0 0 1 0-1.5h11.69l-3.22-3.22a.75.75 0 0 1 0-1.06Zm-7.94 9a.75.75 0 0 1 0 1.06l-3.22 3.22H16.5a.75.75 0 0 1 0 1.5H4.81l3.22 3.22a.75.75 0 1 1-1.06 1.06l-4.5-4.5a.75.75 0 0 1 0-1.06l4.5-4.5a.75.75 0 0 1 1.06 0Z\" clip-rule=\"evenodd\"/></svg>","copy":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M17.663 3.118c.225.015.45.032.673.05C19.876 3.298 21 4.604 21 6.109v9.642a3 3 0 0 1-3 3V16.5c0-5.922-4.576-10.775-10.384-11.217.324-1.132 1.3-2.01 2.548-2.114.224-.019.448-.036.673-.051A3 3 0 0 1 13.5 1.5H15a3 3 0 0 1 2.663 1.618ZM12 4.5A1.5 1.5 0 0 1 13.5 3H15a1.5 1.5 0 0 1 1.5 1.5H12Z\" clip-rule=\"evenodd\"/> <path d=\"M3 8.625c0-1.036.84-1.875 1.875-1.875h.375A3.75 3.75 0 0 1 9 10.5v1.875c0 1.036.84 1.875 1.875 1.875h1.875A3.75 3.75 0 0 1 16.5 18v2.625c0 1.035-.84 1.875-1.875 1.875h-9.75A1.875 1.875 0 0 1 3 20.625v-12Z\"/> <path d=\"M10.5 10.5a5.23 5.23 0 0 0-1.279-3.434 9.768 9.768 0 0 1 6.963 6.963 5.23 5.23 0 0 0-3.434-1.279h-1.875a.375.375 0 0 1-.375-.375V10.5Z\"/></svg>","back":"<svg class=\"fn-svg-icon fn-svg-hero\" aria-hidden=\"true\" viewBox=\"0 0 24 24\" fill=\"currentColor\"><path fill-rule=\"evenodd\" d=\"M11.03 3.97a.75.75 0 0 1 0 1.06l-6.22 6.22H21a.75.75 0 0 1 0 1.5H4.81l6.22 6.22a.75.75 0 1 1-1.06 1.06l-7.5-7.5a.75.75 0 0 1 0-1.06l7.5-7.5a.75.75 0 0 1 1.06 0Z\" clip-rule=\"evenodd\"/></svg>"}};

  const ICON_PACKS = {
    "emoji": { label: "Forge Emoji (Default)", mode: "emoji" },
    "windows": { label: "Windows theme (Fluent UI)", mode: "real-svg" },
    "ios": { label: "IOS theme (Ionicons)", mode: "real-svg" },
    "aws": { label: "AWS theme (Cloudscape)", mode: "real-svg" },
    "android": { label: "Android theme (Material Rounded)", mode: "real-svg" },
    "lucide-clean": { label: "Lucide Outline (ISC)", mode: "real-svg" },
    "hero-solid": { label: "Heroicons Solid (MIT)", mode: "real-svg" },
    "terminal-ascii": { label: "Terminal / ASCII", mode: "ascii" }
  };

  const UI_STYLE_ALIASES = {
    "fluent-windows": "windows",
    "cupertino-apple": "ios",
    "cloudscape-aws": "aws"
  };

  const ICON_PACK_ALIASES = {
    "fluent-line": "windows",
    "cupertino-soft": "ios",
    "cloudscape-sharp": "aws"
  };

  function resolveUiStyleId(styleId) {
    const mapped = UI_STYLE_ALIASES[styleId] || styleId;
    return UI_STYLES[mapped] ? mapped : "forge-classic";
  }

  function resolveIconPackId(packId) {
    const mapped = ICON_PACK_ALIASES[packId] || packId;
    return ICON_PACKS[mapped] ? mapped : "emoji";
  }

  const EMOJI_KEYS = Object.keys(EMOJI_TO_KEY).sort((a, b) => b.length - a.length);
  const EMOJI_REGEX = new RegExp(
    "(" + EMOJI_KEYS.map((e) => e.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")).join("|") + ")",
    "g"
  );

  let currentColorTheme = "github-dark";
  let currentUiStyle = "forge-classic";
  let currentIconPack = "emoji";
  let iconObserver = null;
  let isApplyingIcons = false;

  function ensureSharedStyleSheet() {
    let styleEl = document.getElementById("forge-theme-overrides");
    if (!styleEl) {
      styleEl = document.createElement("style");
      styleEl.id = "forge-theme-overrides";
      (document.head || document.documentElement).appendChild(styleEl);
    }
    styleEl.textContent = `
      .fn-icon-slot {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        vertical-align: middle;
        line-height: 1;
        flex-shrink: 0;
      }
      .fn-svg-icon {
        width: 1.15em;
        height: 1.15em;
        display: inline-block;
        vertical-align: -0.15em;
        flex-shrink: 0;
      }
      .fn-svg-cloudscape {
        fill: none;
      }
      .fn-svg-cloudscape * {
        stroke: currentColor;
        stroke-width: 1.65;
      }
      .fn-svg-cloudscape .filled {
        fill: currentColor;
      }
      .fn-svg-cloudscape .no-stroke {
        stroke: none;
      }
      .fn-svg-cloudscape .stroke-linejoin-round {
        stroke-linejoin: round;
      }
      .fn-svg-cloudscape .stroke-linecap-square {
        stroke-linecap: square;
      }
      .fn-svg-cloudscape .stroke-linecap-round {
        stroke-linecap: round;
      }
      .fn-ascii-icon {
        font-family: var(--font-mono);
        font-size: 0.85em;
        font-weight: 700;
        letter-spacing: -0.02em;
        opacity: 0.9;
      }

      /* Apply UI style tokens consistently across all Forge Node pages */
      .card, .modal {
        border-radius: var(--fn-radius-lg, 10px) !important;
        border-width: var(--fn-border-width, 1px) !important;
        box-shadow: var(--fn-shadow-card, none) !important;
      }
      .subcard, .node-card, .code-panel, .diag-item, .file-item, .user-row,
      #localMqttDetailsBox, #remoteMqttMonitorNotice, #aauLogsOutput {
        border-radius: var(--fn-radius-md, 8px) !important;
        border-width: var(--fn-border-width, 1px) !important;
        box-shadow: var(--fn-shadow-card, none) !important;
      }
      .btn, .input, .select, .status-banner, .toast, .node-item, .program-block,
      .sub-table-wrap, .chip, .code-tab, .section-tab {
        border-radius: var(--fn-radius-sm, 6px) !important;
        border-width: var(--fn-border-width, 1px) !important;
      }
      .btn {
        box-shadow: var(--fn-shadow-btn, none) !important;
      }
      .btn-secondary:hover:not(:disabled) {
        background-color: var(--bg-hover) !important;
      }
      .file-status, .validation-pill, .aau-status-pill, .stream-toggle-pill,
      .badge, .subcard-badge, .diag-count {
        border-radius: var(--fn-radius-pill, 999px) !important;
      }
      .brand-logo-img, .hub-logo-img {
        border-radius: var(--fn-radius-md, 8px) !important;
      }
      .hub-title {
        background: linear-gradient(90deg, var(--text-main) 0%, var(--primary) 55%, var(--purple) 100%) !important;
        -webkit-background-clip: text !important;
        -webkit-text-fill-color: transparent !important;
        text-shadow: none !important;
      }

      /* Windows theme polish */
      html[data-ui-style="windows"] .btn-primary {
        border-bottom: 2px solid rgba(0, 0, 0, 0.28) !important;
      }
      html[data-ui-style="windows"] .subcard,
      html[data-ui-style="windows"] .card {
        backdrop-filter: saturate(140%) blur(8px);
      }

      /* IOS theme polish */
      html[data-ui-style="ios"] .btn {
        border-radius: 999px !important;
        font-weight: 600;
        padding-left: 14px;
        padding-right: 14px;
      }
      html[data-ui-style="ios"] .btn-sm {
        padding-left: 11px;
        padding-right: 11px;
      }
      html[data-ui-style="ios"] .card,
      html[data-ui-style="ios"] .subcard,
      html[data-ui-style="ios"] .modal {
        backdrop-filter: blur(14px) saturate(160%);
      }

      /* AWS theme polish */
      html[data-ui-style="aws"] .subcard-top,
      html[data-ui-style="aws"] .card-header {
        border-bottom: 1px solid var(--border);
        padding-bottom: 8px;
      }
      html[data-ui-style="aws"] .btn {
        border-radius: 16px !important;
        font-weight: 700;
        letter-spacing: 0.01em;
      }
      html[data-ui-style="aws"] .input,
      html[data-ui-style="aws"] .select {
        border-radius: 4px !important;
      }

      /* Android theme polish (Material 3 rounded tonal surfaces) */
      html[data-ui-style="android"] .btn {
        border-radius: 999px !important;
        font-weight: 500;
        letter-spacing: 0.015em;
        padding-left: 15px;
        padding-right: 15px;
      }
      html[data-ui-style="android"] .btn-sm {
        padding-left: 12px;
        padding-right: 12px;
      }
      html[data-ui-style="android"] .subcard {
        padding: 16px !important;
      }

      /* IDE Compact polish */
      html[data-ui-style="ide-compact"] .subcard {
        padding: 11px 12px !important;
        gap: 8px !important;
      }
      html[data-ui-style="ide-compact"] .btn-sm {
        padding: 3px 8px !important;
      }

      /* Sharp Terminal polish */
      html[data-ui-style="sharp-terminal"] .card-title,
      html[data-ui-style="sharp-terminal"] .brand-title,
      html[data-ui-style="sharp-terminal"] .hub-title {
        letter-spacing: 0.02em;
      }

      /* Neubrutalist polish */
      html[data-ui-style="neubrutalist"] .btn:active:not(:disabled) {
        transform: translate(1px, 1px);
        box-shadow: 1px 1px 0 var(--border) !important;
      }
    `;
  }

  function renderIconHtml(key, packId) {
    const pack = ICON_PACKS[packId] || ICON_PACKS["emoji"];
    if (pack.mode === "ascii") {
      const txt = KEY_TO_ASCII[key] || "[*]";
      return `<span class="fn-ascii-icon">${txt}</span>`;
    }
    if (pack.mode === "real-svg") {
      const packMap = REAL_SVG_PACKS[packId];
      if (packMap && packMap[key]) {
        return packMap[key];
      }
    }
    return KEY_TO_EMOJI[key] || "";
  }

  function updateIconSlot(slot, packId) {
    const key = slot.getAttribute("data-fn-icon");
    if (!key) return;
    const html = renderIconHtml(key, packId);
    if (slot.getAttribute("data-rendered-pack") === packId && slot.innerHTML === html) {
      return;
    }
    slot.setAttribute("data-rendered-pack", packId);
    slot.innerHTML = html;
  }

  const SKIP_TAGS = new Set(["SCRIPT", "STYLE", "TEXTAREA", "INPUT", "SELECT", "OPTION", "CODE", "PRE", "SVG"]);

  function shouldSkipNode(node) {
    let el = node.nodeType === 1 ? node : node.parentElement;
    while (el) {
      if (SKIP_TAGS.has(el.tagName) || (el.classList && el.classList.contains("fn-icon-slot"))) {
        return true;
      }
      el = el.parentElement;
    }
    return false;
  }

  function wrapEmojisInRoot(rootNode) {
    if (!rootNode) return;
    if (rootNode.nodeType === 1 && rootNode.hasAttribute && rootNode.hasAttribute("data-fn-icon")) {
      rootNode.classList.add("fn-icon-slot");
      updateIconSlot(rootNode, currentIconPack);
      return;
    }
    const walker = document.createTreeWalker(rootNode, NodeFilter.SHOW_TEXT, null);
    const textNodes = [];
    let n;
    while ((n = walker.nextNode())) {
      if (!shouldSkipNode(n) && n.nodeValue && EMOJI_REGEX.test(n.nodeValue)) {
        textNodes.push(n);
      }
      EMOJI_REGEX.lastIndex = 0;
    }

    for (const tn of textNodes) {
      const text = tn.nodeValue;
      EMOJI_REGEX.lastIndex = 0;
      const frag = document.createDocumentFragment();
      let lastIdx = 0;
      let match;
      while ((match = EMOJI_REGEX.exec(text)) !== null) {
        if (match.index > lastIdx) {
          frag.appendChild(document.createTextNode(text.slice(lastIdx, match.index)));
        }
        const emoji = match[0];
        const key = EMOJI_TO_KEY[emoji];
        const span = document.createElement("span");
        span.className = "fn-icon-slot";
        span.setAttribute("data-fn-icon", key);
        updateIconSlot(span, currentIconPack);
        frag.appendChild(span);
        lastIdx = match.index + emoji.length;
      }
      if (lastIdx < text.length) {
        frag.appendChild(document.createTextNode(text.slice(lastIdx)));
      }
      if (tn.parentNode) {
        tn.parentNode.replaceChild(frag, tn);
      }
    }

    if (rootNode.querySelectorAll) {
      rootNode.querySelectorAll("[data-fn-icon]").forEach((slot) => {
        slot.classList.add("fn-icon-slot");
        updateIconSlot(slot, currentIconPack);
      });
    }
  }

  function applyIconPack(packId) {
    const resolved = resolveIconPackId(packId);
    currentIconPack = resolved;
    const root = document.documentElement;
    root.setAttribute("data-icon-pack", resolved);
    try {
      localStorage.setItem(ICON_STORAGE_KEY, resolved);
    } catch (_) {}

    const sel = document.getElementById("selIconPack");
    if (sel && sel.value !== resolved) {
      sel.value = resolved;
    }

    if (document.body) {
      isApplyingIcons = true;
      try {
        wrapEmojisInRoot(document.body);
      } finally {
        isApplyingIcons = false;
      }
    }
  }

  function startIconObserver() {
    if (iconObserver || !document.body || typeof MutationObserver === "undefined") return;
    iconObserver = new MutationObserver((mutations) => {
      if (isApplyingIcons) return;
      isApplyingIcons = true;
      try {
        for (const m of mutations) {
          if (m.type === "characterData" && m.target) {
            wrapEmojisInRoot(m.target.parentNode || m.target);
          } else if (m.type === "childList") {
            m.addedNodes.forEach((added) => wrapEmojisInRoot(added));
          }
        }
      } finally {
        isApplyingIcons = false;
      }
    });
    iconObserver.observe(document.body, { childList: true, subtree: true, characterData: true });
  }

  function applyThemeVariables(themeId) {
    const resolved = THEMES[themeId] ? themeId : "github-dark";
    currentColorTheme = resolved;
    const theme = THEMES[resolved];
    const root = document.documentElement;
    for (const [k, v] of Object.entries(theme.vars)) {
      root.style.setProperty(k, v);
    }
    root.style.colorScheme = theme.scheme || "dark";
    root.setAttribute("data-theme", resolved);
    root.setAttribute("data-color-scheme", theme.scheme || "dark");
    try {
      localStorage.setItem(COLOR_STORAGE_KEY, resolved);
    } catch (_) {}
    const sel = document.getElementById("selColorTheme");
    if (sel && sel.value !== resolved) {
      sel.value = resolved;
    }
  }

  function applyUiStyle(styleId) {
    const resolved = resolveUiStyleId(styleId);
    currentUiStyle = resolved;
    const style = UI_STYLES[resolved];
    const root = document.documentElement;
    for (const [k, v] of Object.entries(style.vars)) {
      root.style.setProperty(k, v);
    }
    root.setAttribute("data-ui-style", resolved);
    try {
      localStorage.setItem(STYLE_STORAGE_KEY, resolved);
    } catch (_) {}
    const sel = document.getElementById("selUiStyle");
    if (sel && sel.value !== resolved) {
      sel.value = resolved;
    }
  }

  function applyAllAppearance({ colorTheme, uiStyle, iconPack } = {}) {
    ensureSharedStyleSheet();
    if (colorTheme) applyThemeVariables(colorTheme);
    if (uiStyle) applyUiStyle(uiStyle);
    if (iconPack) applyIconPack(iconPack);
  }

  async function saveAppearanceToServer() {
    try {
      await fetch("/api/theme", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          theme: currentColorTheme,
          colorTheme: currentColorTheme,
          uiStyle: currentUiStyle,
          iconPack: currentIconPack
        })
      });
    } catch (_) {}
  }

  async function setAndPersistTheme(themeId) {
    applyThemeVariables(themeId);
    await saveAppearanceToServer();
  }

  async function setAndPersistUiStyle(styleId) {
    applyUiStyle(styleId);
    await saveAppearanceToServer();
  }

  async function setAndPersistIconPack(packId) {
    applyIconPack(packId);
    await saveAppearanceToServer();
  }

  // Apply cached appearance immediately to avoid unstyled flash
  ensureSharedStyleSheet();
  try {
    const cachedColor = localStorage.getItem(COLOR_STORAGE_KEY);
    if (cachedColor && THEMES[cachedColor]) applyThemeVariables(cachedColor);
    const cachedStyle = localStorage.getItem(STYLE_STORAGE_KEY);
    if (cachedStyle) applyUiStyle(cachedStyle);
    const cachedIcon = localStorage.getItem(ICON_STORAGE_KEY);
    if (cachedIcon) currentIconPack = resolveIconPackId(cachedIcon);
  } catch (_) {}

  // Fetch authoritative theme config from AAU/script/themes/theme-config.conf
  async function syncThemeFromServer() {
    try {
      const res = await fetch("/api/theme", { cache: "no-store" });
      if (res.ok) {
        const data = await res.json();
        const color = data.colorTheme || data.theme;
        if (color && THEMES[color]) applyThemeVariables(color);
        if (data.uiStyle) applyUiStyle(data.uiStyle);
        if (data.iconPack) applyIconPack(data.iconPack);
      }
    } catch (_) {}
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => {
      ensureSharedStyleSheet();
      applyIconPack(currentIconPack);
      startIconObserver();
    });
  } else {
    ensureSharedStyleSheet();
    applyIconPack(currentIconPack);
    startIconObserver();
  }

  window.ForgeNodeTheme = {
    THEMES,
    UI_STYLES,
    ICON_PACKS,
    applyThemeVariables,
    applyUiStyle,
    applyIconPack,
    applyAllAppearance,
    setAndPersistTheme,
    setAndPersistUiStyle,
    setAndPersistIconPack,
    syncThemeFromServer,
    getState: () => ({
      colorTheme: currentColorTheme,
      uiStyle: currentUiStyle,
      iconPack: currentIconPack
    })
  };

  syncThemeFromServer();
})();
