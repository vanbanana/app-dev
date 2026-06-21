"use client";

import { useRef, type CSSProperties, type ReactNode } from "react";
import { useEditor, useValue } from "tldraw";
import {
  MousePointer2,
  Hand,
  ImagePlus,
  Frame,
  Square,
  Pen,
  Type,
  Sparkles,
} from "lucide-react";
import { addImageFromFile, createImageGenNode } from "./canvas-actions";

export function Toolbar() {
  const editor = useEditor();
  const fileRef = useRef<HTMLInputElement>(null);
  const tool = useValue("tool", () => editor.getCurrentToolId(), [editor]);

  const stop = (e: React.PointerEvent) => e.stopPropagation();

  return (
    <div style={S.wrap} onPointerDown={stop}>
      <div style={S.bar}>
        <Btn active={tool === "select"} onClick={() => editor.setCurrentTool("select")} title="选择">
          <MousePointer2 size={18} />
        </Btn>
        <Btn active={tool === "hand"} onClick={() => editor.setCurrentTool("hand")} title="拖拽画布">
          <Hand size={18} />
        </Btn>
        <Btn onClick={() => fileRef.current?.click()} title="上传图片">
          <ImagePlus size={18} />
        </Btn>
        <Btn active={tool === "frame"} onClick={() => editor.setCurrentTool("frame")} title="画框">
          <Frame size={18} />
        </Btn>
        <Btn active={tool === "geo"} onClick={() => editor.setCurrentTool("geo")} title="矩形">
          <Square size={18} />
        </Btn>
        <Btn active={tool === "draw"} onClick={() => editor.setCurrentTool("draw")} title="画笔">
          <Pen size={18} />
        </Btn>
        <Btn active={tool === "text"} onClick={() => editor.setCurrentTool("text")} title="文本">
          <Type size={18} />
        </Btn>

        <div style={S.divider} />

        <button
          style={S.aiBtn}
          title="新建图像生成节点"
          onClick={() => {
            const id = createImageGenNode(editor);
            editor.select(id);
          }}
        >
          <Sparkles size={18} />
        </button>
      </div>

      <input
        ref={fileRef}
        type="file"
        accept="image/*"
        style={{ display: "none" }}
        onChange={(e) => {
          const file = e.target.files?.[0];
          if (file) void addImageFromFile(editor, file);
          e.target.value = "";
        }}
      />
    </div>
  );
}

function Btn({
  children,
  onClick,
  active,
  title,
}: {
  children: ReactNode;
  onClick: () => void;
  active?: boolean;
  title: string;
}) {
  return (
    <button
      title={title}
      onClick={onClick}
      style={{ ...S.btn, ...(active ? S.btnActive : null) }}
    >
      {children}
    </button>
  );
}

const S: Record<string, CSSProperties> = {
  wrap: {
    position: "absolute",
    bottom: 18,
    left: 0,
    right: 0,
    display: "flex",
    justifyContent: "center",
    pointerEvents: "none",
    zIndex: 300,
  },
  bar: {
    pointerEvents: "all",
    display: "flex",
    alignItems: "center",
    gap: 4,
    padding: 6,
    borderRadius: 16,
    background: "var(--bar-grad)",
    border: "1px solid var(--border)",
    boxShadow: "var(--shadow-panel)",
  },
  btn: {
    width: 38,
    height: 38,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 10,
    border: "none",
    background: "transparent",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
  btnActive: { background: "var(--bg-hover)", color: "var(--text)" },
  divider: { width: 1, height: 22, background: "var(--border)", margin: "0 4px" },
  aiBtn: {
    width: 38,
    height: 38,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 10,
    border: "1px solid var(--border-strong)",
    background: "var(--btn-soft)",
    color: "var(--text)",
    boxShadow: "var(--shadow-soft)",
    cursor: "pointer",
  },
};
