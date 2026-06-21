"use client";

/* eslint-disable @next/next/no-img-element */
import { useEffect, useRef, useState, type CSSProperties, type ReactNode } from "react";
import type { Editor } from "tldraw";
import {
  ImagePlus,
  Sparkles,
  Loader2,
  RefreshCw,
  AlertCircle,
  Wand2,
  Scissors,
  Download,
  FolderArchive,
  Share2,
  Clock,
  Layers,
  Check,
} from "lucide-react";
import {
  frameHeight,
  ratioToSize,
  totalHeight,
  type ImageGenShape,
} from "./ImageGenShapeUtil";
import { cropViews, VIEW_LABELS_CN } from "@/lib/crop";
import { SKILLS, getSkill, isThreeView, type SkillId } from "@/lib/skills";
import {
  downloadSingleView,
  downloadAllViews,
  downloadViewsZip,
  downloadDataUrl,
  shareImage,
} from "@/lib/download";
import { runGeneration } from "@/lib/generation";
import { CropOverlay } from "../CropOverlay";

const RATIOS = ["3:2", "2:3", "1:1", "16:9"];

// Stop tldraw from hijacking pointer + keyboard events on interactive controls.
const interactive = {
  "data-interactive": true,
  onPointerDown: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerMove: (e: React.PointerEvent) => e.stopPropagation(),
  onPointerUp: (e: React.PointerEvent) => e.stopPropagation(),
  onKeyDown: (e: React.KeyboardEvent) => e.stopPropagation(),
  onWheel: (e: React.WheelEvent) => e.stopPropagation(),
} as const;

function baseName(shape: ImageGenShape): string {
  return `three_view_${shape.id.replace("shape:", "").slice(0, 8)}`;
}

export function ImageGenNode({ shape, editor }: { shape: ImageGenShape; editor: Editor }) {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [crops, setCrops] = useState<string[]>([]);
  const [showCrop, setShowCrop] = useState(false);
  const p = shape.props;
  const presentation = p.presentation === true;
  const threeView = isThreeView(p.skill);
  const fH = frameHeight(p.w, p.ratio);

  // Keep the shape's box height in sync with its borderless layout (also
  // self-heals nodes persisted under an older height formula).
  useEffect(() => {
    const want = totalHeight(p.w, p.ratio, p.status === "done" && threeView, presentation);
    if (Math.abs(want - p.h) > 1) {
      editor.updateShape<ImageGenShape>({ id: shape.id, type: "image-gen", props: { h: want } });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [p.w, p.ratio, p.status, presentation, threeView]);

  // Recompute the three cropped thumbnails whenever the source/splits change.
  useEffect(() => {
    let cancelled = false;
    if (p.status === "done" && p.imageUrl) {
      cropViews(p.imageUrl, p.splits)
        .then((c) => {
          if (!cancelled) setCrops(c);
        })
        .catch(() => {
          if (!cancelled) setCrops([]);
        });
    } else {
      setCrops([]);
    }
    return () => {
      cancelled = true;
    };
  }, [p.status, p.imageUrl, p.splits]);

  function patch(props: Partial<ImageGenShape["props"]>) {
    editor.updateShape<ImageGenShape>({ id: shape.id, type: "image-gen", props });
  }

  function setRatio(ratio: string) {
    patch({ ratio, h: totalHeight(p.w, ratio, p.status === "done" && threeView, presentation) });
  }

  function setSkill(skill: SkillId) {
    patch({ skill });
  }

  function applyManualSplits(splits: number[]) {
    patch({ splits });
    setShowCrop(false);
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
    await runGeneration(editor, shape.id);
  }

  const busy = p.status === "generating" || p.status === "queued";

  return (
    <div style={S.rootBare}>
      {/* Title row (full editor card only) */}
      {!presentation && (
        <div style={S.titleRow}>
          <span style={S.titleLeft}>
            <Sparkles size={13} style={{ color: "var(--text-dim)" }} />
            Image Generator
          </span>
          <span style={S.dims}>{ratioToSize(p.ratio).replace("x", " × ")}</span>
        </div>
      )}

      {/* Frame */}
      <div style={{ ...S.frame, height: fH, marginTop: presentation ? 0 : 10 }}>
        {p.status === "done" && p.imageUrl ? (
          <img src={p.imageUrl} alt="生成结果" style={S.image} draggable={false} />
        ) : p.status === "generating" ? (
          <div style={S.center}>
            <Loader2 size={26} style={{ color: "var(--text-dim)", animation: "spin 1s linear infinite" }} />
            <span style={S.hint}>{threeView ? "正在生成三视图…" : "正在生成…"}</span>
          </div>
        ) : p.status === "queued" ? (
          <div style={S.center}>
            <Clock size={24} style={{ color: "var(--text-dim)" }} />
            <span style={S.hint}>排队中…</span>
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
        {p.status === "done" && !threeView && p.imageUrl && (
          <div style={S.imgActions} {...interactive}>
            <ActionIcon title="下载" onClick={() => downloadDataUrl(p.imageUrl, `${baseName(shape)}.png`)}>
              <Download size={14} />
            </ActionIcon>
            <ActionIcon title="分享" onClick={() => void shareImage(p.imageUrl, baseName(shape))}>
              <Share2 size={14} />
            </ActionIcon>
          </div>
        )}
      </div>

      {/* Cropped views strip (three-view skills only) */}
      {p.status === "done" && threeView && (
        <div style={S.cropsWrap} {...interactive}>
          <div style={S.cropsHeader}>
            <span style={S.cropsTitle}>三视图</span>
            <div style={S.cropActions}>
              <ActionIcon title="调整裁切" onClick={() => setShowCrop(true)}>
                <Scissors size={14} />
              </ActionIcon>
              <ActionIcon title="全部下载" onClick={() => void downloadAllViews(p.imageUrl, p.splits, baseName(shape))}>
                <Download size={14} />
              </ActionIcon>
              <ActionIcon title="打包 ZIP" onClick={() => void downloadViewsZip(p.imageUrl, p.splits, baseName(shape))}>
                <FolderArchive size={14} />
              </ActionIcon>
              <ActionIcon title="分享" onClick={() => void shareImage(p.imageUrl, baseName(shape))}>
                <Share2 size={14} />
              </ActionIcon>
            </div>
          </div>
          <div style={S.cropsRow}>
            {[0, 1, 2].map((i) => (
              <button
                key={i}
                {...interactive}
                style={S.cropCell}
                title={`下载${VIEW_LABELS_CN[i]}`}
                onClick={() => void downloadSingleView(p.imageUrl, p.splits, i, baseName(shape))}
              >
                <div style={S.cropThumb}>
                  {crops[i] ? (
                    <img src={crops[i]} alt={VIEW_LABELS_CN[i]} style={S.cropImg} draggable={false} />
                  ) : (
                    <Loader2 size={16} style={{ color: "var(--text-faint)", animation: "spin 1s linear infinite" }} />
                  )}
                </div>
                <span style={S.cropLabel}>{VIEW_LABELS_CN[i]}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      {showCrop && (
        <CropOverlay
          imageUrl={p.imageUrl}
          splits={p.splits}
          onSave={applyManualSplits}
          onClose={() => setShowCrop(false)}
        />
      )}

      {/* Prompt panel (full editor card only) */}
      {!presentation && (
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
            <SkillPicker value={p.skill ?? "general"} onChange={setSkill} />
            <Dropdown value={p.ratio} options={RATIOS} onChange={setRatio} />
          </div>

          <div style={S.right}>
            <span style={S.model}>
              <Wand2 size={13} style={{ color: "var(--text-dim)" }} /> GPT Image
            </span>
            <button {...interactive} onClick={generate} style={S.genBtn} disabled={busy}>
              {busy ? (
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
      )}
    </div>
  );
}

function ActionIcon({ children, onClick, title }: { children: ReactNode; onClick: () => void; title: string }) {
  return (
    <button {...interactive} style={S.actionIcon} onClick={onClick} title={title}>
      {children}
    </button>
  );
}

function SkillPicker({
  value,
  onChange,
}: {
  value: SkillId;
  onChange: (v: SkillId) => void;
}) {
  const [open, setOpen] = useState(false);
  const wrapRef = useRef<HTMLDivElement>(null);
  const current = getSkill(value);

  useEffect(() => {
    if (!open) return;
    const onDown = (e: PointerEvent) => {
      if (!wrapRef.current?.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", onDown);
    return () => document.removeEventListener("pointerdown", onDown);
  }, [open]);

  return (
    <div ref={wrapRef} style={S.skillWrap} {...interactive}>
      <button {...interactive} style={S.skillBtn} onClick={() => setOpen(!open)} title="选择 Skill">
        <Layers size={13} style={{ color: "var(--text-dim)" }} />
        {current.label}
      </button>
      {open && (
        <div style={S.skillMenu} {...interactive}>
          {SKILLS.map((s) => (
            <button
              key={s.id}
              {...interactive}
              style={{ ...S.skillItem, ...(s.id === value ? S.skillItemActive : null) }}
              onClick={() => {
                onChange(s.id);
                setOpen(false);
              }}
            >
              <span style={S.skillItemMain}>
                <span style={S.skillItemLabel}>{s.label}</span>
                <span style={S.skillItemDesc}>{s.desc}</span>
              </span>
              {s.id === value && <Check size={14} style={{ color: "var(--text)" }} />}
            </button>
          ))}
        </div>
      )}
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
      <select {...interactive} value={value} onChange={(e) => onChange(e.target.value)} style={S.select}>
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
    padding: 18,
    boxSizing: "border-box",
    borderRadius: 18,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-panel)",
  },
  rootBare: {
    width: "100%",
    height: "100%",
    display: "flex",
    flexDirection: "column",
    fontSize: 13,
    color: "var(--text)",
    userSelect: "none",
    boxSizing: "border-box",
  },
  titleRow: {
    height: 24,
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
    marginTop: 10,
    width: "100%",
    borderRadius: 12,
    background: "transparent",
    overflow: "hidden",
    position: "relative",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  imgActions: { position: "absolute", top: 8, right: 8, display: "flex", gap: 6, zIndex: 2 },
  image: { width: "100%", height: "100%", objectFit: "contain", background: "#fff" },
  center: { display: "flex", flexDirection: "column", alignItems: "center", gap: 10 },
  hint: { fontSize: 12, color: "var(--text-dim)" },

  cropsWrap: { marginTop: 12, height: 100, display: "flex", flexDirection: "column", gap: 8 },
  cropsHeader: { display: "flex", alignItems: "center", justifyContent: "space-between" },
  cropsTitle: { fontSize: 12, fontWeight: 500, color: "var(--text-dim)" },
  cropActions: { display: "flex", alignItems: "center", gap: 4 },
  actionIcon: {
    width: 26,
    height: 26,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 7,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
  cropsRow: { display: "flex", gap: 8, flex: 1, minHeight: 0 },
  cropCell: {
    flex: 1,
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: 4,
    border: "none",
    background: "transparent",
    cursor: "pointer",
    padding: 0,
  },
  cropThumb: {
    width: "100%",
    flex: 1,
    minHeight: 0,
    borderRadius: 8,
    background: "#fff",
    border: "1px solid var(--border)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
  },
  cropImg: { width: "100%", height: "100%", objectFit: "contain" },
  cropLabel: { fontSize: 10.5, color: "var(--text-faint)" },

  panel: {
    marginTop: 14,
    flex: 1,
    borderRadius: 14,
    background: "var(--panel-grad)",
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
  skillWrap: { position: "relative", display: "inline-flex" },
  skillBtn: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    background: "var(--bg-elevated)",
    border: "1px solid var(--border)",
    borderRadius: 9,
    padding: "5px 10px",
    fontSize: 12,
    color: "var(--text)",
    cursor: "pointer",
    whiteSpace: "nowrap",
  },
  skillMenu: {
    position: "absolute",
    bottom: "calc(100% + 8px)",
    left: 0,
    minWidth: 196,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 12,
    padding: 6,
    boxShadow: "var(--shadow-panel)",
    display: "flex",
    flexDirection: "column",
    gap: 2,
    zIndex: 30,
  },
  skillItem: {
    display: "flex",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 10,
    width: "100%",
    border: "none",
    background: "transparent",
    borderRadius: 8,
    padding: "7px 9px",
    cursor: "pointer",
    textAlign: "left",
  },
  skillItemActive: { background: "var(--bg-elevated)" },
  skillItemMain: { display: "flex", flexDirection: "column", gap: 1 },
  skillItemLabel: { fontSize: 12.5, color: "var(--text)" },
  skillItemDesc: { fontSize: 10.5, color: "var(--text-faint)" },
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
    background: "var(--btn-primary)",
    color: "var(--btn-primary-text)",
    fontSize: 12.5,
    fontWeight: 600,
    padding: "7px 14px",
    cursor: "pointer",
    boxShadow: "var(--shadow-btn)",
  },
};
