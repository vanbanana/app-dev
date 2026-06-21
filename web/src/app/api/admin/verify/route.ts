import { NextRequest, NextResponse } from "next/server";
import { isAdmin } from "@/lib/admin-auth";

export const runtime = "nodejs";

export async function POST(req: NextRequest) {
  if (!isAdmin(req)) return NextResponse.json({ error: "密码错误" }, { status: 401 });
  return NextResponse.json({ ok: true });
}
