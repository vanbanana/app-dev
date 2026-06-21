import { NextRequest, NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { genConfigPublic, setConfigValue, deleteConfigValue } from "@/lib/db";

export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  if (!isAdmin(req)) return NextResponse.json({ error: "未授权" }, { status: 401 });
  return NextResponse.json({ config: genConfigPublic() });
}

export async function POST(req: NextRequest) {
  if (!isAdmin(req)) return NextResponse.json({ error: "未授权" }, { status: 401 });

  let body: { apiKey?: string; baseUrl?: string; model?: string };
  try {
    body = (await req.json()) as typeof body;
  } catch {
    return NextResponse.json({ error: "请求格式错误" }, { status: 400 });
  }

  // Each field: a non-empty string sets a DB override; an explicit empty string
  // clears the override (falling back to env/default); undefined leaves as-is.
  if (body.apiKey !== undefined) {
    const v = body.apiKey.trim();
    if (v) setConfigValue("api_key", v);
    else deleteConfigValue("api_key");
  }
  if (body.baseUrl !== undefined) {
    const v = body.baseUrl.trim().replace(/\/$/, "");
    if (v) setConfigValue("base_url", v);
    else deleteConfigValue("base_url");
  }
  if (body.model !== undefined) {
    const v = body.model.trim();
    if (v) setConfigValue("model", v);
    else deleteConfigValue("model");
  }

  return NextResponse.json({ config: genConfigPublic() });
}
