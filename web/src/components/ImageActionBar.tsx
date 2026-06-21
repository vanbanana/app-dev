"use client";

import { useState, type CSSProperties, type ReactNode } from "react";
import {
  createShapeId,
  useEditor,
  useValue,
  type TLImageShape,
  type TLShapeId,
} from "tldraw";
import { Sparkles, Scissors, Download, Share2, Loader2 } from "lucide-react";
import { NODE_W, totalHeight, type ImageGenShape } from "./shapes/ImageGenShapeUtil";
import { getSettings } from "@/lib/settings";
import { runGeneration } from "@/lib/generation";
import { detectSplits } from "@/lib/crop";
import { downloadDataUrl, shareImage } from "@/lib/download";

const stop = {
  onPointerDown: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerUp: (e: React.PointerEvent) => e.stopPropagation(),
} as const;

/**
 * Floating action bar that hovers above a single selected *uploaded* image
 * (a native tldraw image shape). It exposes our per-image functions —
 * generate three-view, auto-crop, download, share — and shows a right-aligned
 * spinner while a three-view generation spawned from this image is running.
 */
export function ImageActionBar() {
  const editor = useEditor();
  const [pending, setPending] = useState<{ imgId: TLShapeId; nodeId: TLShapeId } | null>(null);

  const sel = useValue(
    "image-selection",
    () => {
      const ids = editor.getSelectedShapeIds();
      if (ids.length !== 1) return null;
      const shape = editor.getShape(ids[0]);
      if (!shape || shape.type !== "image") return null;
      const b = editor.getShapePageBounds(shape.id);
      if (!b) return null;
      const tl = editor.pageToScreen({ x: b.minX, y: b.minY });
      const tr = editor.pageToScreen({ x: b.maxX, y: b.minY });
      return { id: shape.id, midX: (tl.x + tr.x) / 2, top: tl.y };
    },
    [editor],
  );

  const busy = useValue(
    "image-busy",
    () => {
      if (!pending) return false;
      const n = editor.getShape<ImageGenShape>(pending.nodeId);
      if (!n) return false;
      return n.props.status === "queued" || n.props.status === "generating";
    },
    [editor, pending],
  );

  if (!sel) return null;

  function srcOf(id: TLShapeId): string | null {
    const shape = editor.getShape<TLImageShape>(id);
    if (!shape) return null;
    const asset = editor.getAsset(shape.props.assetId!);
    return asset?.props.src ?? null;
  }

  function spawnBelow(id: TLShapeId): { x: number; y: number } {
    const b = editor.getShapePageBounds(id)!;
    return { x: b.minX, y: b.maxY + 48 };
  }

  function generateThreeView() {
    const src = srcOf(sel!.id);
    if (!src) return;
    const { defaultStyle, defaultRatio } = getSettings();
    const { x, y } = spawnBelow(sel!.id);
    const nodeId = createShapeId();
    editor.createShape<ImageGenShape>({
      id: nodeId,
      type: "image-gen",
      x,
      y,
      props: {
        w: NODE_W,
        h: totalHeight(NODE_W, defaultRatio, false, true),
        style: defaultStyle,
        skill: defaultStyle,
        ratio: defaultRatio,
        referenceImage: src,
        presentation: true,
        createdAt: Date.now(),
      },
    });
    setPending({ imgId: sel!.id, nodeId });
    void runGeneration(editor, nodeId);
  }

  async function autoCrop() {
    const src = srcOf(sel!.id);
    if (!src) return;
    const { defaultRatio } = getSettings();
    const { x, y } = spawnBelow(sel!.id);
    const nodeId = createShapeId();
    let splits: number[] = [];
    try {
      splits = await detectSplits(src);
    } catch {
      splits = [];
    }
    editor.createShape<ImageGenShape>({
      id: nodeId,
      type: "image-gen",
      x,
      y,
      props: {
        w: NODE_W,
        h: totalHeight(NODE_W, defaultRatio, true, true),
        ratio: defaultRatio,
        skill: "realistic",
        status: "done",
        imageUrl: src,
        splits,
        presentation: true,
        createdAt: Date.now(),
      },
    });
    editor.select(nodeId);
  }

  function download() {
    const src = srcOf(sel!.id);
    if (src) downloadDataUrl(src, `image_${String(sel!.id).slice(-6)}.png`);
  }

  function share() {
    const src = srcOf(sel!.id);
    if (src) void shareImage(src, `image_${String(sel!.id).slice(-6)}`);
  }

  const showSpinner = busy && pending?.imgId === sel.id;

  return (
    <div
      style={{ ...S.wrap, left: sel.midX, top: sel.top - 14 }}
      {...stop}
      onWheel={(e) => e.stopPropagation()}
    >
      <button style={{ ...S.btn, ...S.primary }} onClick={generateThreeView} disabled={showSpinner}>
        <Sparkles size={14} />
        生成三视图
      </button>
      <span style={S.sep} />
      <Btn onClick={() => void autoCrop()} title="自动裁切">
        <Scissors size={14} />
        自动裁切
      </Btn>
      <Btn onClick={download} title="下载">
        <Download size={14} />
      </Btn>
      <Btn onClick={share} title="分享">
        <Share2 size={14} />
      </Btn>
      {showSpinner && (
        <span style={S.spinner}>
          <Loader2 size={15} style={{ animation: "spin 1s linear infinite" }} />
        </span>
      )}
    </div>
  );
}

function Btn({ children, onClick, title }: { children: ReactNode; onClick: () => void; title: string }) {
  return (
    <button style={S.btn} onClick={onClick} title={title}>
      {children}
    </button>
  );
}

const S: Record<string, CSSProperties> = {
  wrap: {
    position: "fixed",
    zIndex: 400,
    transform: "translate(-50%, -100%)",
    display: "flex",
    alignItems: "center",
    gap: 4,
    padding: 6,
    borderRadius: 14,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-panel)",
    pointerEvents: "all",
  },
  btn: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    height: 32,
    padding: "0 11px",
    borderRadius: 9,
    border: "none",
    background: "transparent",
    color: "var(--text-dim)",
    fontSize: 12.5,
    fontWeight: 500,
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  primary: {
    background: "var(--btn-primary)",
    color: "var(--btn-primary-text)",
    fontWeight: 600,
  },
  sep: { width: 1, height: 18, background: "var(--border)", margin: "0 2px" },
  spinner: {
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    width: 30,
    height: 30,
    marginLeft: 2,
    color: "var(--text-dim)",
  },
};
