import { NextRequest } from "next/server";

export function adminPassword(): string {
  return process.env.ADMIN_PASSWORD || "admin";
}

export function isAdmin(req: NextRequest): boolean {
  const provided = req.headers.get("x-admin-password") ?? "";
  const expected = adminPassword();
  if (provided.length !== expected.length) return false;
  let diff = 0;
  for (let i = 0; i < expected.length; i++) diff |= provided.charCodeAt(i) ^ expected.charCodeAt(i);
  return diff === 0;
}

const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no ambiguous 0/O/1/I

export function randomCode(len = 8): string {
  let out = "";
  for (let i = 0; i < len; i++) out += ALPHABET[Math.floor(Math.random() * ALPHABET.length)];
  return out;
}
