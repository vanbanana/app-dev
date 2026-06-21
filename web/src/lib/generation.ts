import type { Editor } from "tldraw";
import type { ImageGenShape } from "@/components/shapes/ImageGenShapeUtil";
import { ratioToSize, totalHeight } from "@/components/shapes/ImageGenShapeUtil";
import { detectSplits } from "./crop";
import { getSettings } from "./settings";

// ---- concurrency limiter -------------------------------------------------
let active = 0;
const queue: Array<() => void> = [];

function pump(): void {
  while (active < getSettings().concurrency && queue.length > 0) {
    const run = queue.shift();
    if (!run) break;
    active++;
    run();
  }
}

function enqueue<T>(fn: () => Promise<T>): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    queue.push(() => {
      fn()
        .then(resolve, reject)
        .finally(() => {
          active--;
          setTimeout(pump, getSettings().scheduleDelay);
        });
    });
    pump();
  });
}

// ---- shared generation routine ------------------------------------------
/**
 * Runs three-view generation for a single node, respecting the global
 * concurrency limit. Both the node's "generate" button and batch upload
 * funnel through here so queuing/concurrency behave consistently.
 */
export async function runGeneration(editor: Editor, id: ImageGenShape["id"]): Promise<void> {
  const shape = editor.getShape<ImageGenShape>(id);
  if (!shape) return;
  if (shape.props.status === "generating" || shape.props.status === "queued") return;

  editor.updateShape<ImageGenShape>({
    id,
    type: "image-gen",
    props: { status: "queued", error: "", h: totalHeight(shape.props.w, shape.props.ratio, false) },
  });

  await enqueue(async () => {
    const s = editor.getShape<ImageGenShape>(id);
    if (!s) return;
    editor.updateShape<ImageGenShape>({ id, type: "image-gen", props: { status: "generating", error: "" } });
    try {
      const res = await fetch("/api/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          style: s.props.style,
          prompt: s.props.prompt,
          size: ratioToSize(s.props.ratio),
          referenceImage: s.props.referenceImage || undefined,
        }),
      });
      const data = (await res.json()) as { image?: string; error?: string };
      if (!res.ok || !data.image) throw new Error(data.error || `请求失败 (${res.status})`);

      const props: Partial<ImageGenShape["props"]> = {
        status: "done",
        imageUrl: data.image,
        splits: [],
        h: totalHeight(s.props.w, s.props.ratio, true),
      };
      if (getSettings().autoCrop) {
        try {
          props.splits = await detectSplits(data.image);
        } catch {
          props.splits = [];
        }
      }
      editor.updateShape<ImageGenShape>({ id, type: "image-gen", props });
    } catch (err) {
      editor.updateShape<ImageGenShape>({
        id,
        type: "image-gen",
        props: { status: "error", error: err instanceof Error ? err.message : "生成失败" },
      });
    }
  });
}
