"use client";

/* eslint-disable @next/next/no-img-element */
import { useEffect, useRef, useState, type CSSProperties } from "react";
import { createPortal } from "react-dom";
import { X, Check, RotateCcw } from "lucide-react";

const LABELS = ["正面图", "侧面图", "俯视图"];

/**
 * Full-screen overlay for manually adjusting the two vertical split lines that
 * divide a three-view sheet into front / side / top. Mirrors the original
 * Android ManualCropScreen (two draggable vertical lines).
 */
export function CropOverlay({
  imageUrl,
  splits,
  onSave,
  onClose,
}: {
  imageUrl: string;
  splits: number[];
  onSave: (splits: number[]) => void;
  onClose: () => void;
}) {
  const init = splits.length === 2 ? splits : [1 / 3, 2 / 3];
  const [s1, setS1] = useState(init[0]);
  const [s2, setS2] = useState(init[1]);
  const boxRef = useRef<HTMLDivElement>(null);
  const dragging = useRef<0 | 1 | null>(null);

  useEffect(() => {
    function move(e: PointerEvent) {
      if (dragging.current === null || !boxRef.current) return;
      const rect = boxRef.current.getBoundingClientRect();
      const frac = Math.min(1, Math.max(0, (e.clientX - rect.left) / rect.width));
      if (dragging.current === 0) setS1(Math.min(frac, s2 - 0.02));
      else setS2(Math.max(frac, s1 + 0.02));
    }
    function up() {
      dragging.current = null;
    }
    window.addEventListener("pointermove", move);
    window.addEventListener("pointerup", up);
    return () => {
      window.removeEventListener("pointermove", move);
      window.removeEventListener("pointerup", up);
    };
  }, [s1, s2]);

  const overlay = (
    <div style={S.backdrop} onPointerDown={onClose}>
      <div style={S.modal} onPointerDown={(e) => e.stopPropagation()}>
        <div style={S.header}>
          <span style={S.title}>调整裁切</span>
          <button style={S.iconBtn} onClick={onClose} title="关闭">
            <X size={16} />
          </button>
        </div>

        <div ref={boxRef} style={S.imageBox}>
          <img src={imageUrl} alt="三视图" style={S.image} draggable={false} />
          {[s1, s2].map((s, i) => (
            <div
              key={i}
              style={{ ...S.line, left: `${s * 100}%` }}
              onPointerDown={(e) => {
                e.preventDefault();
                dragging.current = i as 0 | 1;
              }}
            >
              <div style={S.handle} />
            </div>
          ))}
          {/* region labels */}
          {[s1 / 2, (s1 + s2) / 2, (s2 + 1) / 2].map((c, i) => (
            <span key={i} style={{ ...S.regionLabel, left: `${c * 100}%` }}>
              {LABELS[i]}
            </span>
          ))}
        </div>

        <div style={S.footer}>
          <button
            style={S.resetBtn}
            onClick={() => {
              setS1(1 / 3);
              setS2(2 / 3);
            }}
          >
            <RotateCcw size={14} /> 均分三等分
          </button>
          <div style={{ flex: 1 }} />
          <button style={S.cancelBtn} onClick={onClose}>
            取消
          </button>
          <button style={S.saveBtn} onClick={() => onSave([s1, s2])}>
            <Check size={14} /> 应用裁切
          </button>
        </div>
      </div>
    </div>
  );

  if (typeof document === "undefined") return null;
  return createPortal(overlay, document.body);
}

const S: Record<string, CSSProperties> = {
  backdrop: {
    position: "fixed",
    inset: 0,
    background: "rgba(0, 0, 0, 0.72)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    zIndex: 1000,
  },
  modal: {
    width: "min(880px, 92vw)",
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 16,
    boxShadow: "var(--shadow-panel)",
    padding: 18,
    display: "flex",
    flexDirection: "column",
    gap: 14,
  },
  header: { display: "flex", alignItems: "center", justifyContent: "space-between" },
  title: { fontSize: 15, fontWeight: 600, color: "var(--text)" },
  iconBtn: {
    width: 30,
    height: 30,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
  imageBox: {
    position: "relative",
    width: "100%",
    background: "#fff",
    borderRadius: 10,
    overflow: "hidden",
    userSelect: "none",
    touchAction: "none",
  },
  image: { width: "100%", display: "block", pointerEvents: "none" },
  line: {
    position: "absolute",
    top: 0,
    bottom: 0,
    width: 2,
    marginLeft: -1,
    background: "#1a1a1a",
    cursor: "ew-resize",
    boxShadow: "0 0 0 1px rgba(255,255,255,0.6)",
  },
  handle: {
    position: "absolute",
    top: "50%",
    left: "50%",
    width: 16,
    height: 40,
    transform: "translate(-50%, -50%)",
    borderRadius: 6,
    background: "#1a1a1a",
    border: "2px solid #fff",
    cursor: "ew-resize",
  },
  regionLabel: {
    position: "absolute",
    bottom: 8,
    transform: "translateX(-50%)",
    fontSize: 11,
    fontWeight: 600,
    color: "#1a1a1a",
    background: "rgba(255,255,255,0.82)",
    padding: "2px 8px",
    borderRadius: 6,
    pointerEvents: "none",
  },
  footer: { display: "flex", alignItems: "center", gap: 10 },
  resetBtn: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    height: 36,
    padding: "0 12px",
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text-dim)",
    fontSize: 12.5,
    cursor: "pointer",
  },
  cancelBtn: {
    height: 36,
    padding: "0 14px",
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "transparent",
    color: "var(--text-dim)",
    fontSize: 12.5,
    cursor: "pointer",
  },
  saveBtn: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    height: 36,
    padding: "0 16px",
    borderRadius: 9,
    border: "none",
    background: "var(--btn-primary)",
    color: "var(--btn-primary-text)",
    fontSize: 12.5,
    fontWeight: 600,
    cursor: "pointer",
    boxShadow: "var(--shadow-btn)",
  },
};
