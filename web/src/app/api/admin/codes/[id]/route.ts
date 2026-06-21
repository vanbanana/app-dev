import { NextRequest, NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";
import { adminView, deleteCode, updateCode } from "@/lib/db";

export const runtime = "nodejs";

export async function PATCH(req: NextRequest, ctx: { params: Promise<{ id: string }> }) {
  if (!isAdmin(req)) return NextResponse.json({ error: "未授权" }, { status: 401 });
  const { id } = await ctx.params;
  const numId = Number(id);
  if (!Number.isInteger(numId)) return NextResponse.json({ error: "无效 ID" }, { status: 400 });

  let body: { quota?: number; enabled?: boolean; note?: string };
  try {
    body = (await req.json()) as typeof body;
  } catch {
    return NextResponse.json({ error: "请求格式错误" }, { status: 400 });
  }

  const row = updateCode(numId, body);
  if (!row) return NextResponse.json({ error: "未找到该邀请码" }, { status: 404 });
  return NextResponse.json({ code: adminView(row) });
}

export async function DELETE(req: NextRequest, ctx: { params: Promise<{ id: string }> }) {
  if (!isAdmin(req)) return NextResponse.json({ error: "未授权" }, { status: 401 });
  const { id } = await ctx.params;
  const numId = Number(id);
  if (!Number.isInteger(numId)) return NextResponse.json({ error: "无效 ID" }, { status: 400 });
  deleteCode(numId);
  return NextResponse.json({ ok: true });
}
