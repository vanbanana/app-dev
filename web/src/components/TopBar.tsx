"use client";

import { useState, type CSSProperties } from "react";
import { ChevronDown, Zap, MessagesSquare, Box, History, Settings } from "lucide-react";
import { setOpenPanel, useOpenPanel } from "@/lib/ui-store";

export function TopBar() {
  const [name, setName] = useState("Untitled");
  const open = useOpenPanel();

  return (
    <div style={S.wrap}>
      <div style={S.left}>
        <div style={S.logo}>
          <Box size={16} style={{ color: "var(--text)" }} />
        </div>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          onPointerDown={(e) => e.stopPropagation()}
          style={S.title}
          spellCheck={false}
        />
        <ChevronDown size={15} style={{ color: "var(--text-faint)" }} />
      </div>

      <div style={S.right}>
        <button
          style={{ ...S.iconPill, ...(open === "history" ? S.iconPillActive : null) }}
          onPointerDown={(e) => e.stopPropagation()}
          onClick={() => setOpenPanel("history")}
          title="历史记录"
        >
          <History size={15} />
        </button>
        <button
          style={{ ...S.iconPill, ...(open === "settings" ? S.iconPillActive : null) }}
          onPointerDown={(e) => e.stopPropagation()}
          onClick={() => setOpenPanel("settings")}
          title="设置"
        >
          <Settings size={15} />
        </button>
        <div style={S.credits}>
          <Zap size={13} style={{ color: "#cfcfd3" }} fill="#cfcfd3" />
          <span style={{ fontVariantNumeric: "tabular-nums" }}>30</span>
        </div>
        <div style={S.avatar}>A</div>
        <button style={S.chat} onPointerDown={(e) => e.stopPropagation()}>
          <MessagesSquare size={14} />
          对话
        </button>
      </div>
    </div>
  );
}

const pill: CSSProperties = {
  display: "inline-flex",
  alignItems: "center",
  gap: 6,
  height: 34,
  borderRadius: 10,
  background: "var(--bar-grad)",
  border: "1px solid var(--border)",
  boxShadow: "var(--shadow-soft)",
  padding: "0 11px",
  fontSize: 13,
  color: "var(--text)",
};

const S: Record<string, CSSProperties> = {
  wrap: {
    position: "absolute",
    top: 14,
    left: 16,
    right: 16,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    pointerEvents: "none",
    zIndex: 300,
  },
  left: { ...pill, pointerEvents: "all", gap: 8 },
  logo: {
    width: 22,
    height: 22,
    borderRadius: 7,
    background: "var(--bg-hover)",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
  },
  title: {
    background: "transparent",
    border: "none",
    outline: "none",
    color: "var(--text)",
    fontSize: 13.5,
    fontWeight: 500,
    width: 96,
  },
  right: { display: "flex", alignItems: "center", gap: 10, pointerEvents: "all" },
  iconPill: {
    width: 34,
    height: 34,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 10,
    background: "var(--bar-grad)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-soft)",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
  iconPillActive: { background: "var(--bg-hover)", color: "var(--text)" },
  credits: { ...pill, gap: 6, fontWeight: 600 },
  avatar: {
    width: 30,
    height: 30,
    borderRadius: "50%",
    background: "#33333a",
    border: "1px solid var(--border-strong)",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: 13,
    fontWeight: 600,
    color: "#fff",
  },
  chat: {
    ...pill,
    cursor: "pointer",
    fontWeight: 500,
    color: "var(--text-dim)",
  },
};
