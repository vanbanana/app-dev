"use client";

/* eslint-disable @next/next/no-img-element */
import { type CSSProperties } from "react";
import { createShapeId, useValue, type Editor } from "tldraw";
import { Layers, Loader2, Sparkles, Check, AlertCircle, RotateCcw, Clock } from "lucide-react";
import {
  NODE_W,
  totalHeight,
  type ImageGenShape,
  type ImageGenStatus,
  type ImageGenStyle,
} from "./ImageGenShapeUtil";
import { BATCH_LAYOUT, visibleCells, type BatchGenShape } from "./BatchGenShapeUtil";
import { runGeneration } from "@/lib/generation";

const RATIOS = ["3:2", "2:3", "1:1", "16:9"];
const STYLE_OPTIONS: { value: ImageGenStyle; label: string }[] = [
  { value: "realistic", label: "写实" },
  { value: "chibi", label: "Q版" },
];

const interactive = {
  "data-interactive": true,
  onPointerDown: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerMove: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerUp: (e: React.PointerEvent) => e.stopPropagation(),
  onKeyDown: (e: React.KeyboardEvent) => e.stopPropagation(),
  onWheel: (e: React.WheelEvent) => e.stopPropagation(),
} as const;

export function BatchGenNode({ shape, editor }: { shape: BatchGenShape; editor: Editor }) {
  const p = shape.props;
  const { THUMB, THUMB_GAP, COLS } = BATCH_LAYOUT;
  const count = p.images.length;

  // Live per-child generation statuses (1:1 with images by index).
  const statuses = useValue(
    "batch-statuses",
    () => {
      const sp = editor.getShape<BatchGenShape>(shape.id)?.props;
      const ids = sp?.childIds ?? [];
      return ids.map(
        (id) => editor.getShape<ImageGenShape>(id as ImageGenShape["id"])?.props.status ?? "idle",
      );
    },
    [editor, shape.id],
  );

  const done = statuses.filter((s) => s === "done").length;
  const errored = statuses.filter((s) => s === "error").length;
  const running = statuses.some((s) => s === "generating" || s === "queued");

  function patch(props: Partial<BatchGenShape["props"]>) {
    editor.updateShape<BatchGenShape>({ id: shape.id, type: "batch-gen", props });
  }

  function startBatch() {
    const s = editor.getShape<BatchGenShape>(shape.id);
    if (!s || s.props.started || s.props.images.length === 0) return;
    const { images, style, ratio } = s.props;
    const childH = totalHeight(NODE_W, ratio, false, true);
    const cols = Math.min(images.length, 3);
    const gapX = NODE_W + 56;
    // rows must clear the node's *final* height (it grows once the三视图 crops appear)
    const gapY = totalHeight(NODE_W, ratio, true, true) + 72;
    const startX = s.x;
    const startY = s.y + s.props.h + 64;

    const ids = images.map((img, i) => {
      const id = createShapeId();
      const col = i % cols;
      const row = Math.floor(i / cols);
      editor.createShape<ImageGenShape>({
        id,
        type: "image-gen",
        x: startX + col * gapX,
        y: startY + row * gapY,
        props: { w: NODE_W, h: childH, style, ratio, referenceImage: img, presentation: true, createdAt: Date.now() },
      });
      return id;
    });

    patch({ started: true, childIds: ids.map(String) });
    ids.forEach((id) => void runGeneration(editor, id));
  }

  function retryFailed() {
    const ids = editor.getShape<BatchGenShape>(shape.id)?.props.childIds ?? [];
    ids.forEach((id, i) => {
      if (statuses[i] === "error") void runGeneration(editor, id as ImageGenShape["id"]);
    });
  }

  function locate(i: number) {
    const ids = editor.getShape<BatchGenShape>(shape.id)?.props.childIds ?? [];
    const id = ids[i] as ImageGenShape["id"] | undefined;
    if (!id || !editor.getShape(id)) return;
    editor.select(id);
    editor.zoomToSelection({ animation: { duration: 250 } });
  }

  const cells = visibleCells(count);
  const overflow = count - cells;
  const thumbs = p.images.slice(0, overflow > 0 ? cells - 1 : cells);
  const gridWidth = COLS * THUMB + (COLS - 1) * THUMB_GAP;

  return (
    <div style={S.root}>
      <div style={S.header}>
        <span style={S.title}>
          <Layers size={15} style={{ color: "var(--text-dim)" }} />
          批量生成
        </span>
        <span style={S.count}>{count} 张</span>
      </div>

      <div style={{ ...S.grid, width: gridWidth }} {...interactive}>
        {thumbs.map((src, i) => (
          <Thumb
            key={i}
            src={src}
            status={p.started ? statuses[i] : "idle"}
            started={p.started}
            onClick={() => p.started && locate(i)}
          />
        ))}
        {overflow > 0 && (
          <div style={{ ...S.thumb, ...S.overflow }}>+{overflow}</div>
        )}
      </div>

      {/* Progress / hint */}
      <div style={S.progress}>
        {p.started ? (
          <>
            <div style={S.barTrack}>
              <div style={{ ...S.barFill, width: `${count ? (done / count) * 100 : 0}%` }} />
            </div>
            <span style={S.progressText}>
              {running
                ? `生成中 ${done}/${count}`
                : errored > 0
                  ? `完成 ${done}/${count} · ${errored} 张失败`
                  : `全部完成 ${done}/${count}`}
            </span>
          </>
        ) : (
          <span style={S.hint}>已选 {count} 张，点击右侧开始生成三视图</span>
        )}
      </div>

      {/* Controls */}
      <div style={S.controls} {...interactive}>
        <div style={S.left}>
          <Segmented
            options={STYLE_OPTIONS}
            value={p.style}
            disabled={p.started}
            onChange={(v) => patch({ style: v })}
          />
          <Dropdown
            value={p.ratio}
            options={RATIOS}
            disabled={p.started}
            onChange={(v) => patch({ ratio: v })}
          />
        </div>

        {!p.started ? (
          <button {...interactive} style={S.primary} onClick={startBatch} disabled={count === 0}>
            <Sparkles size={14} />
            开始生成 · {count}
          </button>
        ) : running ? (
          <button {...interactive} style={{ ...S.primary, ...S.primaryDisabled }} disabled>
            <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} />
            生成中
          </button>
        ) : errored > 0 ? (
          <button {...interactive} style={S.primary} onClick={retryFailed}>
            <RotateCcw size={14} />
            重试 {errored} 张
          </button>
        ) : (
          <button {...interactive} style={{ ...S.primary, ...S.primaryDone }} disabled>
            <Check size={14} />
            已完成
          </button>
        )}
      </div>
    </div>
  );
}

function Thumb({
  src,
  status,
  started,
  onClick,
}: {
  src: string;
  status: ImageGenStatus;
  started: boolean;
  onClick: () => void;
}) {
  const dim = started && status !== "done";
  return (
    <button
      {...interactive}
      style={{ ...S.thumb, cursor: started ? "pointer" : "default" }}
      onClick={onClick}
      title={started ? "查看结果" : undefined}
    >
      <img src={src} alt="" draggable={false} style={{ ...S.thumbImg, opacity: dim ? 0.4 : 1 }} />
      {started && status === "queued" && (
        <span style={S.badge}>
          <Clock size={13} />
        </span>
      )}
      {started && status === "generating" && (
        <span style={S.badge}>
          <Loader2 size={13} style={{ animation: "spin 1s linear infinite" }} />
        </span>
      )}
      {started && status === "done" && (
        <span style={{ ...S.cornerBadge, background: "#2f7d4f" }}>
          <Check size={11} style={{ color: "#fff" }} />
        </span>
      )}
      {started && status === "error" && (
        <span style={{ ...S.cornerBadge, background: "#b23b3b" }}>
          <AlertCircle size={11} style={{ color: "#fff" }} />
        </span>
      )}
    </button>
  );
}

function Segmented({
  options,
  value,
  onChange,
  disabled,
}: {
  options: { value: ImageGenStyle; label: string }[];
  value: ImageGenStyle;
  onChange: (v: ImageGenStyle) => void;
  disabled?: boolean;
}) {
  return (
    <div style={{ ...S.segmented, opacity: disabled ? 0.5 : 1 }} {...interactive}>
      {options.map((o) => (
        <button
          key={o.value}
          {...interactive}
          disabled={disabled}
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
  disabled,
}: {
  value: string;
  options: string[];
  onChange: (v: string) => void;
  disabled?: boolean;
}) {
  return (
    <div style={{ ...S.selectWrap, opacity: disabled ? 0.5 : 1 }} {...interactive}>
      <select
        {...interactive}
        value={value}
        disabled={disabled}
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

const S: Record<string, CSSProperties> = {
  root: {
    width: "100%",
    height: "100%",
    display: "flex",
    flexDirection: "column",
    padding: 18,
    borderRadius: 18,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-panel)",
    color: "var(--text)",
    fontSize: 13,
    userSelect: "none",
    boxSizing: "border-box",
  },
  header: { height: 30, display: "flex", alignItems: "center", justifyContent: "space-between" },
  title: { display: "inline-flex", alignItems: "center", gap: 7, fontSize: 13.5, fontWeight: 600 },
  count: {
    fontSize: 11.5,
    color: "var(--text-dim)",
    background: "var(--bg-elevated)",
    border: "1px solid var(--border)",
    borderRadius: 20,
    padding: "2px 10px",
    fontVariantNumeric: "tabular-nums",
  },
  grid: { display: "flex", flexWrap: "wrap", gap: 8, marginTop: 14, alignSelf: "center" },
  thumb: {
    width: 84,
    height: 84,
    position: "relative",
    borderRadius: 12,
    overflow: "hidden",
    border: "1px solid var(--border)",
    background: "#fff",
    padding: 0,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  thumbImg: { width: "100%", height: "100%", objectFit: "cover", transition: "opacity 0.2s" },
  overflow: {
    background: "var(--bg-elevated)",
    color: "var(--text-dim)",
    fontSize: 15,
    fontWeight: 600,
    fontVariantNumeric: "tabular-nums",
  },
  badge: {
    position: "absolute",
    inset: 0,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    color: "var(--text)",
    background: "rgba(20,20,22,0.35)",
  },
  cornerBadge: {
    position: "absolute",
    top: 5,
    right: 5,
    width: 18,
    height: 18,
    borderRadius: 9,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  progress: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    height: 22,
    marginTop: 14,
  },
  barTrack: {
    flex: 1,
    height: 5,
    borderRadius: 3,
    background: "var(--bg-elevated)",
    border: "1px solid var(--border)",
    overflow: "hidden",
  },
  barFill: { height: "100%", background: "var(--text-dim)", transition: "width 0.3s ease" },
  progressText: { fontSize: 11.5, color: "var(--text-dim)", fontVariantNumeric: "tabular-nums", whiteSpace: "nowrap" },
  hint: { fontSize: 12, color: "var(--text-faint)" },
  controls: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 8,
    marginTop: 14,
  },
  left: { display: "flex", alignItems: "center", gap: 8 },
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
  primary: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    border: "none",
    borderRadius: 10,
    background: "var(--btn-primary)",
    color: "var(--btn-primary-text)",
    fontSize: 12.5,
    fontWeight: 600,
    padding: "8px 14px",
    cursor: "pointer",
    boxShadow: "var(--shadow-btn)",
    whiteSpace: "nowrap",
  },
  primaryDisabled: { background: "var(--bg-hover)", color: "var(--text-dim)", boxShadow: "none", cursor: "default" },
  primaryDone: { background: "var(--bg-hover)", color: "var(--text)", boxShadow: "none", cursor: "default" },
};
