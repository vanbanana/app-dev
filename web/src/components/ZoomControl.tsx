"use client";

import { type CSSProperties } from "react";
import { useEditor, useValue } from "tldraw";
import { Minus, Plus, Maximize2 } from "lucide-react";

export function ZoomControl() {
  const editor = useEditor();
  const zoom = useValue("zoom", () => editor.getZoomLevel(), [editor]);

  return (
    <div style={S.wrap} onPointerDown={(e) => e.stopPropagation()}>
      <button style={S.btn} title="缩小" onClick={() => editor.zoomOut(editor.getViewportScreenCenter(), { animation: { duration: 120 } })}>
        <Minus size={15} />
      </button>
      <span style={S.pct}>{Math.round(zoom * 100)}%</span>
      <button style={S.btn} title="放大" onClick={() => editor.zoomIn(editor.getViewportScreenCenter(), { animation: { duration: 120 } })}>
        <Plus size={15} />
      </button>
      <div style={S.divider} />
      <button style={S.btn} title="适应画布" onClick={() => editor.zoomToFit({ animation: { duration: 200 } })}>
        <Maximize2 size={14} />
      </button>
    </div>
  );
}

const S: Record<string, CSSProperties> = {
  wrap: {
    position: "absolute",
    bottom: 18,
    left: 16,
    display: "flex",
    alignItems: "center",
    gap: 2,
    padding: 4,
    borderRadius: 12,
    background: "var(--bar-grad)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-panel)",
    pointerEvents: "all",
    zIndex: 300,
  },
  btn: {
    width: 28,
    height: 28,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    border: "none",
    background: "transparent",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
  pct: {
    minWidth: 42,
    textAlign: "center",
    fontSize: 12.5,
    color: "var(--text)",
    fontVariantNumeric: "tabular-nums",
  },
  divider: { width: 1, height: 18, background: "var(--border)", margin: "0 2px" },
};
