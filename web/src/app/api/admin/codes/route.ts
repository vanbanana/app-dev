import { NextRequest, NextResponse } from "next/server";
import { isAdmin, randomCode } from "@/lib/admin-auth";
import { adminView, createCode, findCode, listCodes } from "@/lib/db";

export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  if (!isAdmin(req)) return NextResponse.json({ error: "未授权" }, { status: 401 });
  return NextResponse.json({ codes: listCodes().map(adminView) });
}

export async function POST(req: NextRequest) {
  if (!isAdmin(req)) return NextResponse.json({ error: "未授权" }, { status: 401 });

  let body: { code?: string; quota?: number; note?: string; count?: number };
  try {
    body = (await req.json()) as typeof body;
  } catch {
    return NextResponse.json({ error: "请求格式错误" }, { status: 400 });
  }

  const quota = Math.max(1, Math.floor(Number(body.quota) || 0));
  if (!quota) return NextResponse.json({ error: "请填写有效的额度" }, { status: 400 });

  const count = Math.min(100, Math.max(1, Math.floor(Number(body.count) || 1)));

  // Manual single code (custom string) vs. batch random generation.
  if (body.code && count === 1) {
    const code = body.code.trim().toUpperCase();
    if (findCode(code)) return NextResponse.json({ error: "该邀请码已存在" }, { status: 409 });
    return NextResponse.json({ created: [adminView(createCode({ code, quota, note: body.note }))] });
  }

  const created = [];
  for (let i = 0; i < count; i++) {
    let code = randomCode();
    let guard = 0;
    while (findCode(code) && guard++ < 20) code = randomCode();
    created.push(adminView(createCode({ code, quota, note: body.note })));
  }
  return NextResponse.json({ created });
}
