"use client";

/* eslint-disable @next/next/no-img-element */
import { useRef, type CSSProperties } from "react";
import type { Editor } from "tldraw";
import { ImagePlus, Sparkles, Loader2, RefreshCw, AlertCircle, Wand2 } from "lucide-react";
import {
  frameHeight,
  ratioToSize,
  totalHeight,
  type ImageGenShape,
  type ImageGenStyle,
} from "./ImageGenShapeUtil";

const RATIOS = ["3:2", "2:3", "1:1", "16:9"];
const STYLE_OPTIONS: { value: ImageGenStyle; label: string }[] = [
  { value: "realistic", label: "写实" },
  { value: "chibi", label: "Q版" },
];

// Stop tldraw from hijacking pointer + keyboard events on interactive controls.
const interactive = {
  "data-interactive": true,
  onPointerDown: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerMove: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerUp: (e: React.PointerEvent) => e.stopPropagation(),
  onKeyDown: (e: React.KeyboardEvent) => e.stopPropagation(),
  onWheel: (e: React.WheelEvent) => e.stopPropagation(),
} as const;

export function ImageGenNode({ shape, editor }: { shape: ImageGenShape; editor: Editor }) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const p = shape.props;
  const fH = frameHeight(p.w, p.ratio);

  function patch(props: Partial<ImageGenShape["props"]>) {
    editor.updateShape<ImageGenShape>({ id: shape.id, type: "image-gen", props });
  }

  function setRatio(ratio: string) {
    patch({ ratio, h: totalHeight(p.w, ratio) });
  }

  function onPickReference(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = () => patch({ referenceImage: String(reader.result) });
    reader.readAsDataURL(file);
    e.target.value = "";
  }

  async function generate() {
    if (p.status === "generating") return;
    patch({ status: "generating", error: "" });
    try {
      const res = await fetch("/api/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          style: p.style,
          prompt: p.prompt,
          size: ratioToSize(p.ratio),
          referenceImage: p.referenceImage || undefined,
        }),
      });
      const data = (await res.json()) as { image?: string; error?: string };
      if (!res.ok || !data.image) throw new Error(data.error || `请求失败 (${res.status})`);
      patch({ status: "done", imageUrl: data.image });
    } catch (err) {
      patch({ status: "error", error: err instanceof Error ? err.message : "生成失败" });
    }
  }

  return (
    <div style={S.root}>
      {/* Title row */}
      <div style={S.titleRow}>
        <span style={S.titleLeft}>
          <Sparkles size={13} style={{ color: "var(--accent)" }} />
          Image Generator
        </span>
        <span style={S.dims}>{ratioToSize(p.ratio).replace("x", " × ")}</span>
      </div>

      {/* Frame */}
      <div style={{ ...S.frame, height: fH }}>
        {p.status === "done" && p.imageUrl ? (
          <img src={p.imageUrl} alt="生成结果" style={S.image} draggable={false} />
        ) : p.status === "generating" ? (
          <div style={S.center}>
            <Loader2 size={26} style={{ color: "var(--accent)", animation: "spin 1s linear infinite" }} />
            <span style={S.hint}>正在生成三视图…</span>
          </div>
        ) : p.status === "error" ? (
          <div style={S.center}>
            <AlertCircle size={24} style={{ color: "#ff6b6b" }} />
            <span style={{ ...S.hint, color: "#ff9b9b", maxWidth: "85%", textAlign: "center" }}>{p.error}</span>
          </div>
        ) : (
          <div style={S.center}>
            <ImagePlus size={26} style={{ color: "var(--text-faint)" }} />
          </div>
        )}
      </div>

      {/* Prompt panel */}
      <div style={S.panel} {...interactive}>
        <div style={S.panelTop}>
          <label style={S.ref} {...interactive}>
            {p.referenceImage ? (
              <img src={p.referenceImage} alt="参考图" style={S.refImg} />
            ) : (
              <>
                <ImagePlus size={15} style={{ color: "var(--text-faint)" }} />
                <span style={S.refLabel}>参考图</span>
              </>
            )}
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              style={{ display: "none" }}
              onChange={onPickReference}
            />
          </label>
          <textarea
            {...interactive}
            value={p.prompt}
            placeholder="今天我们要创作什么"
            onChange={(e) => patch({ prompt: e.target.value })}
            style={S.textarea}
            className="scroll-thin"
          />
        </div>

        <div style={S.controls}>
          <div style={S.segs}>
            <Segmented
              options={STYLE_OPTIONS.map((s) => ({ value: s.value, label: s.label }))}
              value={p.style}
              onChange={(v) => patch({ style: v as ImageGenStyle })}
            />
            <Dropdown value={p.ratio} options={RATIOS} onChange={setRatio} />
          </div>

          <div style={S.right}>
            <span style={S.model}>
              <Wand2 size={13} style={{ color: "var(--text-dim)" }} /> GPT Image
            </span>
            <button {...interactive} onClick={generate} style={S.genBtn} disabled={p.status === "generating"}>
              {p.status === "generating" ? (
                <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} />
              ) : p.status === "done" ? (
                <RefreshCw size={14} />
              ) : (
                <Sparkles size={14} />
              )}
              {p.status === "done" ? "重新生成" : "生成"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function Segmented({
  options,
  value,
  onChange,
}: {
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div style={S.segmented} {...interactive}>
      {options.map((o) => (
        <button
          key={o.value}
          {...interactive}
          onClick={() => onChange(o.value)}
          style={{ ...S.seg, ...(value === o.value ? S.segActive : null) }}
        >
          {o.label}
        </button>
      ))}
    </div>
  );
}

function Dropdown({
  value,
  options,
  onChange,
}: {
  value: string;
  options: string[];
  onChange: (v: string) => void;
}) {
  return (
    <div style={S.selectWrap} {...interactive}>
      <select
        {...interactive}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        style={S.select}
      >
        {options.map((o) => (
          <option key={o} value={o}>
            {o}
          </option>
        ))}
      </select>
    </div>
  );
}

const ICON_BTN: CSSProperties = { display: "inline-flex", alignItems: "center", gap: 6 };

const S: Record<string, CSSProperties> = {
  root: {
    width: "100%",
    height: "100%",
    display: "flex",
    flexDirection: "column",
    fontSize: 13,
    color: "var(--text)",
    userSelect: "none",
  },
  titleRow: {
    height: 22,
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "0 2px",
  },
  titleLeft: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    fontSize: 12.5,
    fontWeight: 500,
    color: "var(--text-dim)",
  },
  dims: { fontSize: 11.5, color: "var(--text-faint)", fontVariantNumeric: "tabular-nums" },
  frame: {
    marginTop: 8,
    width: "100%",
    borderRadius: 12,
    background: "var(--bg-elevated)",
    border: "1px solid var(--border)",
    overflow: "hidden",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  image: { width: "100%", height: "100%", objectFit: "contain", background: "#fff" },
  center: { display: "flex", flexDirection: "column", alignItems: "center", gap: 10 },
  hint: { fontSize: 12, color: "var(--text-dim)" },
  panel: {
    marginTop: 14,
    flex: 1,
    borderRadius: 14,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-panel)",
    padding: 12,
    display: "flex",
    flexDirection: "column",
    gap: 10,
  },
  panelTop: { display: "flex", gap: 10, flex: 1, minHeight: 0 },
  ref: {
    width: 64,
    height: 64,
    flexShrink: 0,
    borderRadius: 10,
    border: "1px dashed var(--border-strong)",
    background: "var(--bg-elevated)",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "center",
    gap: 4,
    cursor: "pointer",
    overflow: "hidden",
  },
  refImg: { width: "100%", height: "100%", objectFit: "cover" },
  refLabel: { fontSize: 11, color: "var(--text-faint)" },
  textarea: {
    flex: 1,
    resize: "none",
    background: "transparent",
    border: "none",
    outline: "none",
    color: "var(--text)",
    fontSize: 13.5,
    lineHeight: 1.5,
    fontFamily: "inherit",
  },
  controls: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 8 },
  segs: { display: "flex", alignItems: "center", gap: 8 },
  segmented: {
    display: "inline-flex",
    background: "var(--bg-elevated)",
    borderRadius: 9,
    padding: 2,
    border: "1px solid var(--border)",
  },
  seg: {
    border: "none",
    background: "transparent",
    color: "var(--text-dim)",
    fontSize: 12,
    padding: "4px 10px",
    borderRadius: 7,
    cursor: "pointer",
  },
  segActive: { background: "var(--bg-hover)", color: "var(--text)" },
  selectWrap: {
    display: "inline-flex",
    alignItems: "center",
    background: "var(--bg-elevated)",
    border: "1px solid var(--border)",
    borderRadius: 9,
    padding: "0 4px",
  },
  select: {
    background: "transparent",
    border: "none",
    outline: "none",
    color: "var(--text-dim)",
    fontSize: 12,
    padding: "5px 4px",
    cursor: "pointer",
    appearance: "none",
  },
  right: { display: "flex", alignItems: "center", gap: 8 },
  model: { ...ICON_BTN, fontSize: 12, color: "var(--text-dim)" },
  genBtn: {
    ...ICON_BTN,
    border: "none",
    borderRadius: 10,
    background: "var(--accent)",
    color: "#fff",
    fontSize: 12.5,
    fontWeight: 600,
    padding: "7px 14px",
    cursor: "pointer",
  },
};
