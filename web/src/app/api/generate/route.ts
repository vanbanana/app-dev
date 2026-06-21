import { NextRequest, NextResponse } from "next/server";
import { buildPrompt, type PromptStyle } from "@/lib/prompts";

export const runtime = "nodejs";
export const maxDuration = 120;

const DEFAULT_BASE_URL = "https://api.bltcy.ai";
const DEFAULT_MODEL = "gpt-image-1";

type GenerateBody = {
  style?: PromptStyle;
  prompt?: string;
  referenceImage?: string; // data URL (data:image/png;base64,....)
  size?: string;
  model?: string;
};

function dataUrlToBuffer(dataUrl: string): { buffer: Buffer; mime: string } {
  const match = /^data:(.+?);base64,(.*)$/.exec(dataUrl);
  if (!match) throw new Error("Invalid reference image data URL");
  return { mime: match[1], buffer: Buffer.from(match[2], "base64") };
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
  const apiKey = process.env.BLTCY_API_KEY;
  if (!apiKey) {
    return NextResponse.json(
      { error: "Server is missing BLTCY_API_KEY. Set it in web/.env.local." },
      { status: 500 },
    );
  }

  const baseUrl = (process.env.BLTCY_BASE_URL ?? DEFAULT_BASE_URL).replace(/\/$/, "");

  let body: GenerateBody;
  try {
    body = (await req.json()) as GenerateBody;
  } catch {
    return NextResponse.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const style: PromptStyle = body.style === "chibi" ? "chibi" : "realistic";
  const size = body.size?.trim() || "1536x1024";
  const model = body.model?.trim() || process.env.IMAGE_MODEL || DEFAULT_MODEL;
  const prompt = buildPrompt(style, body.prompt);

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

      upstream = await fetch(`${baseUrl}/v1/images/edits`, {
        method: "POST",
        headers: { Authorization: `Bearer ${apiKey}` },
        body: form,
      });
    } else {
      upstream = await fetch(`${baseUrl}/v1/images/generations`, {
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
      return NextResponse.json(
        { error: `Upstream error (${upstream.status}): ${text.slice(0, 500)}` },
        { status: 502 },
      );
    }

    const json = JSON.parse(text) as { data?: Array<{ b64_json?: string; url?: string }> };
    const first = json.data?.[0];
    if (!first) {
      return NextResponse.json(
        { error: `Unexpected upstream response: ${text.slice(0, 500)}` },
        { status: 502 },
      );
    }

    const image = await toDataUrl(first);
    return NextResponse.json({ image });
  } catch (err) {
    const message = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: message }, { status: 500 });
  }
}
