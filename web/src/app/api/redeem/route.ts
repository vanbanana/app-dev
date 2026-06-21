import { NextRequest, NextResponse } from "next/server";
import { findCode, publicView } from "@/lib/db";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  let body: { code?: string };
  try {
    body = (await req.json()) as { code?: string };
  } catch {
    return NextResponse.json({ error: "请求格式错误" }, { status: 400 });
  }

  const code = (body.code ?? "").trim();
  if (!code) return NextResponse.json({ error: "请输入邀请码" }, { status: 400 });

  const row = findCode(code);
  if (!row) return NextResponse.json({ error: "邀请码无效" }, { status: 404 });
  if (!row.enabled) return NextResponse.json({ error: "该邀请码已被停用" }, { status: 403 });

  return NextResponse.json({ invite: publicView(row) });
}
