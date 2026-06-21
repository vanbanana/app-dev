"use client";

import { type CSSProperties } from "react";
import { useEditor } from "tldraw";
import { X, ShieldCheck } from "lucide-react";
import { useSettings, updateSettings } from "@/lib/settings";
import { closePanel } from "@/lib/ui-store";
import type { ImageGenStyle } from "./shapes/ImageGenShapeUtil";

const RATIOS = ["3:2", "2:3", "1:1", "16:9"];
const STYLES: { value: ImageGenStyle; label: string }[] = [
  { value: "realistic", label: "写实" },
  { value: "chibi", label: "Q版" },
];

export function SettingsPanel() {
  const editor = useEditor();
  const s = useSettings();

  function clearCanvas() {
    if (!window.confirm("确定清除画布上的所有内容？此操作不可撤销。")) return;
    const ids = editor.getCurrentPageShapes().map((sh) => sh.id);
    if (ids.length) editor.deleteShapes(ids);
    closePanel();
  }

  return (
    <div style={S.drawer} onPointerDown={(e) => e.stopPropagation()} onWheel={(e) => e.stopPropagation()}>
      <div style={S.header}>
        <span style={S.title}>设置</span>
        <div style={{ flex: 1 }} />
        <button style={S.iconBtn} onClick={closePanel} title="关闭">
          <X size={16} />
        </button>
      </div>

      <div style={S.body} className="scroll-thin">
        <Field label="并发数" hint="同时进行的生成任务数量">
          <input
            type="number"
            min={1}
            max={8}
            value={s.concurrency}
            onChange={(e) => updateSettings({ concurrency: clamp(Number(e.target.value), 1, 8) })}
            style={S.input}
          />
        </Field>

        <Field label="调度延迟 (ms)" hint="队列中相邻任务的间隔">
          <input
            type="number"
            min={0}
            max={5000}
            step={100}
            value={s.scheduleDelay}
            onChange={(e) => updateSettings({ scheduleDelay: clamp(Number(e.target.value), 0, 5000) })}
            style={S.input}
          />
        </Field>

        <Field label="自动裁切" hint="生成后自动切出 正/侧/顶 三视图">
          <Toggle on={s.autoCrop} onChange={(v) => updateSettings({ autoCrop: v })} />
        </Field>

        <Field label="默认风格" hint="新建节点的默认风格">
          <div style={S.segmented}>
            {STYLES.map((o) => (
              <button
                key={o.value}
                onClick={() => updateSettings({ defaultStyle: o.value })}
                style={{ ...S.seg, ...(s.defaultStyle === o.value ? S.segActive : null) }}
              >
                {o.label}
              </button>
            ))}
          </div>
        </Field>

        <Field label="默认画幅" hint="新建节点的默认比例">
          <div style={S.selectWrap}>
            <select
              value={s.defaultRatio}
              onChange={(e) => updateSettings({ defaultRatio: e.target.value })}
              style={S.select}
            >
              {RATIOS.map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>
        </Field>

        <div style={S.note}>
          <ShieldCheck size={15} style={{ color: "var(--text-dim)", flexShrink: 0 }} />
          <span>API Key 保存在服务端环境变量中，不在前端暴露，无需在此填写。</span>
        </div>

        <button style={S.dangerBtn} onClick={clearCanvas}>
          清除画布数据
        </button>
      </div>
    </div>
  );
}

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <div style={S.field}>
      <div style={S.fieldText}>
        <span style={S.fieldLabel}>{label}</span>
        {hint && <span style={S.fieldHint}>{hint}</span>}
      </div>
      <div style={S.fieldControl}>{children}</div>
    </div>
  );
}

function Toggle({ on, onChange }: { on: boolean; onChange: (v: boolean) => void }) {
  return (
    <button
      onClick={() => onChange(!on)}
      style={{
        width: 42,
        height: 24,
        borderRadius: 20,
        border: "1px solid var(--border)",
        background: on ? "var(--accent)" : "var(--bg-hover)",
        position: "relative",
        cursor: "pointer",
        transition: "background 0.15s",
      }}
    >
      <span
        style={{
          position: "absolute",
          top: 2,
          left: on ? 20 : 2,
          width: 18,
          height: 18,
          borderRadius: "50%",
          background: on ? "#161618" : "var(--text-dim)",
          transition: "left 0.15s",
        }}
      />
    </button>
  );
}

function clamp(n: number, lo: number, hi: number): number {
  if (Number.isNaN(n)) return lo;
  return Math.min(hi, Math.max(lo, n));
}

const S: Record<string, CSSProperties> = {
  drawer: {
    position: "absolute",
    top: 60,
    right: 16,
    width: 340,
    maxHeight: "calc(100% - 80px)",
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
  header: { display: "flex", alignItems: "center", padding: "14px 14px 10px" },
  title: { fontSize: 14.5, fontWeight: 600, color: "var(--text)" },
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
  body: { padding: "4px 14px 14px", display: "flex", flexDirection: "column", gap: 14, overflowY: "auto" },
  field: { display: "flex", alignItems: "center", justifyContent: "space-between", gap: 12 },
  fieldText: { display: "flex", flexDirection: "column", gap: 2, minWidth: 0 },
  fieldLabel: { fontSize: 13, color: "var(--text)" },
  fieldHint: { fontSize: 11, color: "var(--text-faint)" },
  fieldControl: { flexShrink: 0 },
  input: {
    width: 72,
    height: 32,
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text)",
    fontSize: 13,
    padding: "0 10px",
    outline: "none",
    textAlign: "right",
  },
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
    padding: "5px 12px",
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
    padding: "0 6px",
  },
  select: {
    background: "transparent",
    border: "none",
    outline: "none",
    color: "var(--text)",
    fontSize: 13,
    padding: "7px 4px",
    cursor: "pointer",
    appearance: "none",
  },
  note: {
    display: "flex",
    gap: 8,
    alignItems: "flex-start",
    fontSize: 11.5,
    lineHeight: 1.5,
    color: "var(--text-dim)",
    background: "var(--bg-elevated)",
    border: "1px solid var(--border)",
    borderRadius: 10,
    padding: 10,
  },
  dangerBtn: {
    height: 36,
    borderRadius: 9,
    border: "1px solid #5a2a2a",
    background: "transparent",
    color: "#ff8c8c",
    fontSize: 12.5,
    cursor: "pointer",
  },
};
