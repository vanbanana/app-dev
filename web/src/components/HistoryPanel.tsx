"use client";

/* eslint-disable @next/next/no-img-element */
import { useState, type CSSProperties } from "react";
import { useEditor, useValue } from "tldraw";
import { X, FolderArchive, Crosshair, Trash2, CheckSquare, Square, PackageOpen } from "lucide-react";
import type { ImageGenShape } from "./shapes/ImageGenShapeUtil";
import { STYLE_LABELS } from "@/lib/prompts";
import { downloadViewsZip, downloadBatchZip } from "@/lib/download";
import { closePanel } from "@/lib/ui-store";

function fmtTime(ts: number): string {
  if (!ts) return "";
  const d = new Date(ts);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getMonth() + 1}/${d.getDate()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function HistoryPanel() {
  const editor = useEditor();
  const [selectMode, setSelectMode] = useState(false);
  const [selected, setSelected] = useState<Set<string>>(new Set());

  const tasks = useValue(
    "done-tasks",
    () =>
      editor
        .getCurrentPageShapes()
        .filter((s): s is ImageGenShape => s.type === "image-gen" && (s as ImageGenShape).props.status === "done")
        .sort((a, b) => b.props.createdAt - a.props.createdAt),
    [editor],
  );

  function baseName(s: ImageGenShape) {
    return `three_view_${s.id.replace("shape:", "").slice(0, 8)}`;
  }

  function locate(s: ImageGenShape) {
    editor.select(s.id);
    editor.zoomToSelection({ animation: { duration: 250 } });
    closePanel();
  }

  function remove(s: ImageGenShape) {
    editor.deleteShape(s.id);
    setSelected((prev) => {
      const next = new Set(prev);
      next.delete(s.id);
      return next;
    });
  }

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function downloadAll() {
    if (tasks.length === 0) return;
    await downloadBatchZip(tasks.map((t) => ({ imageUrl: t.props.imageUrl, splits: t.props.splits, name: baseName(t) })));
  }

  async function downloadSelected() {
    const chosen = tasks.filter((t) => selected.has(t.id));
    if (chosen.length === 0) return;
    await downloadBatchZip(chosen.map((t) => ({ imageUrl: t.props.imageUrl, splits: t.props.splits, name: baseName(t) })));
  }

  return (
    <div style={S.drawer} onPointerDown={(e) => e.stopPropagation()} onWheel={(e) => e.stopPropagation()}>
      <div style={S.header}>
        <span style={S.title}>历史记录</span>
        <span style={S.count}>{tasks.length}</span>
        <div style={{ flex: 1 }} />
        <button style={S.headBtn} onClick={() => setSelectMode((v) => !v)} title="多选">
          {selectMode ? <CheckSquare size={15} /> : <Square size={15} />}
        </button>
        <button style={S.iconBtn} onClick={closePanel} title="关闭">
          <X size={16} />
        </button>
      </div>

      <div style={S.actionsBar}>
        {selectMode ? (
          <button style={S.barBtn} onClick={() => void downloadSelected()} disabled={selected.size === 0}>
            <FolderArchive size={14} /> 下载选中 ({selected.size})
          </button>
        ) : (
          <button style={S.barBtn} onClick={() => void downloadAll()} disabled={tasks.length === 0}>
            <PackageOpen size={14} /> 全部打包下载
          </button>
        )}
      </div>

      <div style={S.list} className="scroll-thin">
        {tasks.length === 0 ? (
          <div style={S.empty}>还没有生成记录</div>
        ) : (
          tasks.map((t) => (
            <div
              key={t.id}
              style={{ ...S.row, ...(selected.has(t.id) ? S.rowSelected : null) }}
              onClick={() => (selectMode ? toggle(t.id) : locate(t))}
            >
              {selectMode && (
                <span style={S.check}>
                  {selected.has(t.id) ? <CheckSquare size={16} /> : <Square size={16} />}
                </span>
              )}
              <div style={S.thumb}>
                <img src={t.props.imageUrl} alt="" style={S.thumbImg} draggable={false} />
              </div>
              <div style={S.meta}>
                <span style={S.metaTitle}>{STYLE_LABELS[t.props.style]}</span>
                <span style={S.metaPrompt}>{t.props.prompt || "（无提示词）"}</span>
                <span style={S.metaTime}>{fmtTime(t.props.createdAt)}</span>
              </div>
              {!selectMode && (
                <div style={S.rowActions}>
                  <button
                    style={S.rowBtn}
                    title="定位到画布"
                    onClick={(e) => {
                      e.stopPropagation();
                      locate(t);
                    }}
                  >
                    <Crosshair size={14} />
                  </button>
                  <button
                    style={S.rowBtn}
                    title="打包下载"
                    onClick={(e) => {
                      e.stopPropagation();
                      void downloadViewsZip(t.props.imageUrl, t.props.splits, baseName(t));
                    }}
                  >
                    <FolderArchive size={14} />
                  </button>
                  <button
                    style={S.rowBtn}
                    title="删除"
                    onClick={(e) => {
                      e.stopPropagation();
                      remove(t);
                    }}
                  >
                    <Trash2 size={14} />
                  </button>
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}

const S: Record<string, CSSProperties> = {
  drawer: {
    position: "absolute",
    top: 60,
    right: 16,
    bottom: 16,
    width: 340,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 16,
    boxShadow: "var(--shadow-panel)",
    display: "flex",
    flexDirection: "column",
    pointerEvents: "all",
    zIndex: 320,
    overflow: "hidden",
  },
  header: { display: "flex", alignItems: "center", gap: 8, padding: "14px 14px 10px" },
  title: { fontSize: 14.5, fontWeight: 600, color: "var(--text)" },
  count: {
    fontSize: 11,
    color: "var(--text-dim)",
    background: "var(--bg-hover)",
    borderRadius: 20,
    padding: "1px 8px",
  },
  headBtn: {
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
  actionsBar: { padding: "0 14px 10px" },
  barBtn: {
    width: "100%",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
    height: 34,
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text)",
    fontSize: 12.5,
    cursor: "pointer",
  },
  list: { flex: 1, overflowY: "auto", padding: "0 10px 10px", display: "flex", flexDirection: "column", gap: 8 },
  empty: { textAlign: "center", color: "var(--text-faint)", fontSize: 12.5, padding: "40px 0" },
  row: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    padding: 8,
    borderRadius: 12,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    cursor: "pointer",
  },
  rowSelected: { borderColor: "var(--accent)" },
  check: { color: "var(--text-dim)", display: "inline-flex" },
  thumb: {
    width: 64,
    height: 44,
    flexShrink: 0,
    borderRadius: 8,
    overflow: "hidden",
    background: "#fff",
    border: "1px solid var(--border)",
  },
  thumbImg: { width: "100%", height: "100%", objectFit: "cover" },
  meta: { flex: 1, minWidth: 0, display: "flex", flexDirection: "column", gap: 2 },
  metaTitle: { fontSize: 12.5, fontWeight: 500, color: "var(--text)" },
  metaPrompt: {
    fontSize: 11.5,
    color: "var(--text-dim)",
    whiteSpace: "nowrap",
    overflow: "hidden",
    textOverflow: "ellipsis",
  },
  metaTime: { fontSize: 11, color: "var(--text-faint)", fontVariantNumeric: "tabular-nums" },
  rowActions: { display: "flex", flexDirection: "column", gap: 4 },
  rowBtn: {
    width: 26,
    height: 26,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 7,
    border: "1px solid var(--border)",
    background: "var(--bg-panel)",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
};
