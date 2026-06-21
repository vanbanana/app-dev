// One-off: generate sample three-view sheets for the invite gate showcase.
// Usage: node scripts/gen-showcase.mjs
import { writeFileSync, mkdirSync } from "node:fs";
import path from "node:path";

const KEY = process.env.BLTCY_API_KEY;
const BASE = (process.env.BLTCY_BASE_URL || "https://api.bltcy.ai").replace(/\/$/, "");
const MODEL = process.env.IMAGE_MODEL || "gpt-image-1";

const REALISTIC = `Scene:
Pure white background (#FFFFFF), no environment, no ground plane, no cast shadows.
Soft neutral studio lighting from upper-left to clearly define form and volume.

Subject:
Three orthographic projection views of the FULL BODY character/object, arranged HORIZONTALLY in a single row: Front View (left), Side View (center), Top View (right).
Realistic 3D-model-ready reference with clear volume, neutral gray clay/maquette material.

Constraints:
- Each view occupies EXACTLY one-third of width with EQUAL white gaps.
- Clean sharp silhouette, soft ambient occlusion, smooth surfaces.
- NO background elements, NO ground shadows, NO labels, NO text, NO watermark.
- Output image MUST be 16:9 landscape.`;

const subjects = [
  "a brave knight bear mascot in plate armor",
  "a cute robot companion with rounded panels",
  "a fox warrior mascot with a flowing scarf",
  "a chunky astronaut penguin character",
];

const outDir = path.join(process.cwd(), "public", "showcase");
mkdirSync(outDir, { recursive: true });

async function gen(subject, idx) {
  const prompt = `${REALISTIC}\n\nSubject identity: ${subject}.`;
  const res = await fetch(`${BASE}/v1/images/generations`, {
    method: "POST",
    headers: { Authorization: `Bearer ${KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({ model: MODEL, prompt, size: "1536x1024", n: 1, quality: "high" }),
  });
  const text = await res.text();
  if (!res.ok) throw new Error(`(${res.status}) ${text.slice(0, 300)}`);
  const json = JSON.parse(text);
  const item = json.data?.[0];
  let buf;
  if (item?.b64_json) buf = Buffer.from(item.b64_json, "base64");
  else if (item?.url) buf = Buffer.from(await (await fetch(item.url)).arrayBuffer());
  else throw new Error("no image in response");
  const file = path.join(outDir, `${idx + 1}.png`);
  writeFileSync(file, buf);
  console.log("saved", file, (buf.length / 1024).toFixed(0) + "kb");
}

const results = await Promise.allSettled(subjects.map((s, i) => gen(s, i)));
results.forEach((r, i) => {
  if (r.status === "rejected") console.error("FAILED", i + 1, r.reason?.message || r.reason);
});
