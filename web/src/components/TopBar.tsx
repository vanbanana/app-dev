"use client";

import { useState, type CSSProperties } from "react";
import { ChevronDown, Zap, MessagesSquare, Box } from "lucide-react";

export function TopBar() {
  const [name, setName] = useState("Untitled");

  return (
    <div style={S.wrap}>
      <div style={S.left}>
        <div style={S.logo}>
          <Box size={16} style={{ color: "var(--accent)" }} />
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
        <div style={S.credits}>
          <Zap size={13} style={{ color: "#f5c451" }} fill="#f5c451" />
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
  background: "var(--bg-panel)",
  border: "1px solid var(--border)",
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
  credits: { ...pill, gap: 6, fontWeight: 600 },
  avatar: {
    width: 30,
    height: 30,
    borderRadius: "50%",
    background: "linear-gradient(135deg, var(--accent), #b06eff)",
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
