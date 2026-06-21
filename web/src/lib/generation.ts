import type { Editor } from "tldraw";
import type { ImageGenShape } from "@/components/shapes/ImageGenShapeUtil";
import { ratioToSize, totalHeight } from "@/components/shapes/ImageGenShapeUtil";
import { detectSplits } from "./crop";
import { getSettings } from "./settings";
import { isThreeView } from "./skills";
import { applyInvite, getCode, type Invite } from "./invite";

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

type GenData = { image?: string; error?: string; invite?: Invite };
type GenResult = { ok: boolean; status: number; data: GenData };

/**
 * POST to /api/generate with retries. Image generation goes through heavier
 * upstream endpoints (`/v1/images/edits` for references) and can take a long
 * time; over a tunnel the connection may drop or the proxy may return an HTML
 * gateway/timeout page (502/503/504 or Cloudflare 520-524). Those would break
 * `res.json()` with "Unexpected token '<'". We retry on thrown network errors,
 * on any 5xx status, and on non-JSON bodies, then parse safely.
 */
async function postGenerate(body: string, retries = 2): Promise<GenResult> {
  let lastErr: unknown;
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      const res = await fetch("/api/generate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body,
      });
      const text = await res.text();
      const looksJson =
        (res.headers.get("content-type") || "").includes("application/json") ||
        text.trimStart().startsWith("{");
      if ((res.status >= 500 || !looksJson) && attempt < retries) {
        await sleep(900 * (attempt + 1));
        continue;
      }
      let data: GenData = {};
      try {
        data = text ? (JSON.parse(text) as GenData) : {};
      } catch {
        throw new Error("生成服务暂时不可用，请稍后重试");
      }
      return { ok: res.ok, status: res.status, data };
    } catch (err) {
      lastErr = err;
      if (attempt >= retries) break;
      await sleep(900 * (attempt + 1));
    }
  }
  throw lastErr instanceof Error ? lastErr : new Error("网络请求失败，请重试");
}

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
    props: {
      status: "queued",
      error: "",
      h: totalHeight(shape.props.w, shape.props.ratio, false, shape.props.presentation),
    },
  });

  await enqueue(async () => {
    const s = editor.getShape<ImageGenShape>(id);
    if (!s) return;
    editor.updateShape<ImageGenShape>({ id, type: "image-gen", props: { status: "generating", error: "" } });
    const threeView = isThreeView(s.props.skill);
    try {
      const { ok, status, data } = await postGenerate(
        JSON.stringify({
          skill: s.props.skill ?? "general",
          style: s.props.style,
          prompt: s.props.prompt,
          size: ratioToSize(s.props.ratio),
          referenceImage: s.props.referenceImage || undefined,
          code: getCode() ?? undefined,
        }),
      );
      if (data.invite) applyInvite(data.invite);
      if (!ok || !data.image) throw new Error(data.error || `请求失败 (${status})`);

      const props: Partial<ImageGenShape["props"]> = {
        status: "done",
        imageUrl: data.image,
        splits: [],
        h: totalHeight(s.props.w, s.props.ratio, threeView, s.props.presentation),
      };
      // Only three-view skills get split into Front/Side/Top crops.
      if (threeView && getSettings().autoCrop) {
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
