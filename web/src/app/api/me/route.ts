import { NextRequest, NextResponse } from "next/server";
import { findCode, publicView } from "@/lib/db";

export const runtime = "nodejs";

export async function GET(req: NextRequest) {
  const code = (req.nextUrl.searchParams.get("code") ?? "").trim();
  if (!code) return NextResponse.json({ error: "缺少邀请码" }, { status: 400 });

  const row = findCode(code);
  if (!row) return NextResponse.json({ error: "邀请码无效" }, { status: 404 });

  return NextResponse.json({ invite: publicView(row) });
}
