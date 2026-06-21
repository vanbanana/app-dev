import Database from "better-sqlite3";
import { mkdirSync } from "node:fs";
import path from "node:path";

export type InviteCodeRow = {
  id: number;
  code: string;
  quota: number;
  used: number;
  enabled: number; // 0 | 1
  note: string | null;
  created_at: number;
};

const globalForDb = globalThis as unknown as { __atelierDb?: Database.Database };

function init(): Database.Database {
  const dir = process.env.DATA_DIR
    ? path.resolve(process.env.DATA_DIR)
    : path.join(process.cwd(), "data");
  mkdirSync(dir, { recursive: true });
  const db = new Database(path.join(dir, "app.db"));
  db.pragma("journal_mode = WAL");
  db.exec(`
    CREATE TABLE IF NOT EXISTS invite_codes (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      code       TEXT UNIQUE NOT NULL,
      quota      INTEGER NOT NULL DEFAULT 0,
      used       INTEGER NOT NULL DEFAULT 0,
      enabled    INTEGER NOT NULL DEFAULT 1,
      note       TEXT,
      created_at INTEGER NOT NULL
    );
  `);
  return db;
}

export function getDb(): Database.Database {
  if (!globalForDb.__atelierDb) globalForDb.__atelierDb = init();
  return globalForDb.__atelierDb;
}

export function findCode(code: string): InviteCodeRow | undefined {
  return getDb()
    .prepare("SELECT * FROM invite_codes WHERE code = ?")
    .get(code.trim().toUpperCase()) as InviteCodeRow | undefined;
}

export function listCodes(): InviteCodeRow[] {
  return getDb()
    .prepare("SELECT * FROM invite_codes ORDER BY created_at DESC")
    .all() as InviteCodeRow[];
}

export function createCode(input: { code: string; quota: number; note?: string | null }): InviteCodeRow {
  const code = input.code.trim().toUpperCase();
  getDb()
    .prepare("INSERT INTO invite_codes (code, quota, used, enabled, note, created_at) VALUES (?, ?, 0, 1, ?, ?)")
    .run(code, Math.max(0, Math.floor(input.quota)), input.note?.trim() || null, Date.now());
  return findCode(code)!;
}

export function updateCode(
  id: number,
  patch: { quota?: number; enabled?: boolean; note?: string | null },
): InviteCodeRow | undefined {
  const sets: string[] = [];
  const args: unknown[] = [];
  if (patch.quota !== undefined) {
    sets.push("quota = ?");
    args.push(Math.max(0, Math.floor(patch.quota)));
  }
  if (patch.enabled !== undefined) {
    sets.push("enabled = ?");
    args.push(patch.enabled ? 1 : 0);
  }
  if (patch.note !== undefined) {
    sets.push("note = ?");
    args.push(patch.note?.trim() || null);
  }
  if (sets.length === 0) return getDb().prepare("SELECT * FROM invite_codes WHERE id = ?").get(id) as InviteCodeRow | undefined;
  args.push(id);
  getDb().prepare(`UPDATE invite_codes SET ${sets.join(", ")} WHERE id = ?`).run(...(args as never[]));
  return getDb().prepare("SELECT * FROM invite_codes WHERE id = ?").get(id) as InviteCodeRow | undefined;
}

export function deleteCode(id: number): void {
  getDb().prepare("DELETE FROM invite_codes WHERE id = ?").run(id);
}

/**
 * Atomically consume one unit of quota for a code. Returns the updated row on
 * success, or a reason when it cannot be consumed.
 */
export function consumeQuota(
  code: string,
): { ok: true; row: InviteCodeRow } | { ok: false; reason: "not_found" | "disabled" | "exhausted" } {
  const normalized = code.trim().toUpperCase();
  const db = getDb();
  const tx = db.transaction((c: string) => {
    const row = db.prepare("SELECT * FROM invite_codes WHERE code = ?").get(c) as InviteCodeRow | undefined;
    if (!row) return { ok: false as const, reason: "not_found" as const };
    if (!row.enabled) return { ok: false as const, reason: "disabled" as const };
    if (row.used >= row.quota) return { ok: false as const, reason: "exhausted" as const };
    db.prepare("UPDATE invite_codes SET used = used + 1 WHERE id = ?").run(row.id);
    return { ok: true as const, row: { ...row, used: row.used + 1 } };
  });
  return tx(normalized);
}

/** Give back one consumed unit (e.g. when upstream generation failed). */
export function refundQuota(code: string): InviteCodeRow | undefined {
  const normalized = code.trim().toUpperCase();
  getDb()
    .prepare("UPDATE invite_codes SET used = MAX(0, used - 1) WHERE code = ?")
    .run(normalized);
  return findCode(normalized);
}

export function publicView(row: InviteCodeRow) {
  return {
    code: row.code,
    quota: row.quota,
    used: row.used,
    remaining: Math.max(0, row.quota - row.used),
    enabled: !!row.enabled,
  };
}

export function adminView(row: InviteCodeRow) {
  return {
    id: row.id,
    code: row.code,
    quota: row.quota,
    used: row.used,
    remaining: Math.max(0, row.quota - row.used),
    enabled: !!row.enabled,
    note: row.note,
    createdAt: row.created_at,
  };
}
