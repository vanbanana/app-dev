import { NextRequest, NextResponse } from "next/server";
import { buildPrompt, type PromptStyle } from "@/lib/prompts";
import { getSkill, type SkillId } from "@/lib/skills";
import { consumeQuota, refundQuota, findCode, publicView, getGenConfig } from "@/lib/db";

export const runtime = "nodejs";
export const maxDuration = 120;

type GenerateBody = {
  style?: PromptStyle;
  skill?: SkillId;
  prompt?: string;
  referenceImage?: string; // data URL (data:image/png;base64,....)
  size?: string;
  model?: string;
  code?: string; // invite code
};

const QUOTA_ERRORS: Record<string, string> = {
  not_found: "邀请码无效",
  disabled: "该邀请码已被停用",
  exhausted: "该邀请码的可用次数已用完",
};

function dataUrlToBuffer(dataUrl: string): { buffer: Buffer; mime: string } {
  const match = /^data:(.+?);base64,(.*)$/.exec(dataUrl);
  if (!match) throw new Error("Invalid reference image data URL");
  return { mime: match[1], buffer: Buffer.from(match[2], "base64") };
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms));

/**
 * Fetch with a couple of retries on transient network errors (the upstream
 * proxy occasionally drops connections under concurrent load, surfacing as
 * "fetch failed"). Only retries on thrown network errors, not HTTP statuses.
 */
async function fetchWithRetry(
  input: string,
  init: RequestInit,
  retries = 2,
): Promise<Response> {
  let lastErr: unknown;
  for (let attempt = 0; attempt <= retries; attempt++) {
    try {
      return await fetch(input, init);
    } catch (err) {
      lastErr = err;
      if (attempt < retries) await sleep(600 * (attempt + 1));
    }
  }
  throw lastErr instanceof Error ? lastErr : new Error("网络请求失败");
}

async function toDataUrl(item: { b64_json?: string; url?: string }): Promise<string> {
  if (item.b64_json) return `data:image/png;base64,${item.b64_json}`;
  if (item.url) {
    const res = await fetch(item.url);
    if (!res.ok) throw new Error(`Failed to download generated image (${res.status})`);
    const buf = Buffer.from(await res.arrayBuffer());
    const mime = res.headers.get("content-type") ?? "image/png";
    return `data:${mime};base64,${buf.toString("base64")}`;
  }
  throw new Error("API response item contained no image data");
}

export async function POST(req: NextRequest) {
  const { apiKey, baseUrl, model: configuredModel } = getGenConfig();
  if (!apiKey) {
    return NextResponse.json(
      { error: "尚未配置生图 API Key，请在后台「生图配置」中填写。" },
      { status: 500 },
    );
  }

  let body: GenerateBody;
  try {
    body = (await req.json()) as GenerateBody;
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const code = (body.code ?? "").trim();
  if (!code) {
    return NextResponse.json({ error: "缺少邀请码，请先填写邀请码" }, { status: 401 });
  }

  // Resolve the chosen skill and the final upstream prompt. General生图 sends the
  // user prompt as-is; three-view skills wrap it into the ortho reference sheet.
  const skill = getSkill(body.skill);
  const userPrompt = (body.prompt ?? "").trim();
  let prompt: string;
  if (skill.threeView) {
    prompt = buildPrompt(skill.promptStyle ?? "realistic", userPrompt);
  } else {
    if (!userPrompt && !body.referenceImage) {
      return NextResponse.json({ error: "请输入提示词" }, { status: 400 });
    }
    prompt = userPrompt;
  }

  // Atomically reserve one unit of quota before spending an upstream call.
  const reserve = consumeQuota(code);
  if (!reserve.ok) {
    return NextResponse.json({ error: QUOTA_ERRORS[reserve.reason] }, { status: 403 });
  }

  const size = body.size?.trim() || "1536x1024";
  const model = body.model?.trim() || configuredModel;

  try {
    let upstream: Response;

    if (body.referenceImage) {
      const { buffer, mime } = dataUrlToBuffer(body.referenceImage);
      const form = new FormData();
      form.append("image", new Blob([new Uint8Array(buffer)], { type: mime }), "source.png");
      form.append("prompt", prompt);
      form.append("size", size);
      form.append("model", model);
      form.append("n", "1");

      upstream = await fetchWithRetry(`${baseUrl}/v1/images/edits`, {
        method: "POST",
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
      });
    } else {
      upstream = await fetchWithRetry(`${baseUrl}/v1/images/generations`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${apiKey}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ model, prompt, size, n: 1, quality: "high" }),
      });
    }

    const text = await upstream.text();
    if (!upstream.ok) {
      refundQuota(code);
      return NextResponse.json(
        { error: `Upstream error (${upstream.status}): ${text.slice(0, 500)}` },
        { status: 502 },
      );
    }

    const json = JSON.parse(text) as { data?: Array<{ b64_json?: string; url?: string }> };
    const first = json.data?.[0];
    if (!first) {
      refundQuota(code);
      return NextResponse.json(
        { error: `Unexpected upstream response: ${text.slice(0, 500)}` },
        { status: 502 },
      );
    }

    const image = await toDataUrl(first);
    const row = findCode(code);
    return NextResponse.json({ image, invite: row ? publicView(row) : null });
  } catch (err) {
    refundQuota(code);
    const message = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
