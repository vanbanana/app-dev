"use client";

import { useCallback, useEffect, useState, type CSSProperties } from "react";
import {
  ShieldCheck,
  Loader2,
  Plus,
  Copy,
  Check,
  Trash2,
  Power,
  RefreshCw,
  LogOut,
} from "lucide-react";

type CodeView = {
  id: number;
  code: string;
  quota: number;
  used: number;
  remaining: number;
  enabled: boolean;
  note: string | null;
  createdAt: number;
};

const PW_KEY = "atelier.admin.pw";

export default function AdminPage() {
  const [pw, setPw] = useState("");
  const [authed, setAuthed] = useState(false);
  const [loginError, setLoginError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const saved = sessionStorage.getItem(PW_KEY);
    if (saved) {
      setPw(saved);
      void verify(saved);
    }
  }, []);

  async function verify(password: string) {
    setBusy(true);
    setLoginError("");
    try {
      const res = await fetch("/api/admin/verify", {
        method: "POST",
        headers: { "x-admin-password": password },
      });
      if (res.ok) {
        sessionStorage.setItem(PW_KEY, password);
        setAuthed(true);
      } else {
        setLoginError("密码错误");
        setAuthed(false);
      }
    } catch {
      setLoginError("网络错误");
    } finally {
      setBusy(false);
    }
  }

  function logout() {
    sessionStorage.removeItem(PW_KEY);
    setAuthed(false);
    setPw("");
  }

  if (!authed) {
    return (
      <div style={S.center}>
        <div style={S.loginCard}>
          <div style={S.logo}>
            <ShieldCheck size={22} style={{ color: "var(--text)" }} />
          </div>
          <h1 style={S.title}>后台管理</h1>
          <p style={S.subtitle}>请输入管理员密码</p>
          <input
            type="password"
            autoFocus
            value={pw}
            onChange={(e) => {
              setPw(e.target.value);
              setLoginError("");
            }}
            onKeyDown={(e) => e.key === "Enter" && void verify(pw)}
            placeholder="管理员密码"
            style={S.input}
          />
          {loginError && <div style={S.error}>{loginError}</div>}
          <button style={S.primaryBtn} onClick={() => void verify(pw)} disabled={busy || !pw}>
            {busy ? <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} /> : "登录"}
          </button>
        </div>
      </div>
    );
  }

  return <Dashboard pw={pw} onLogout={logout} />;
}

function Dashboard({ pw, onLogout }: { pw: string; onLogout: () => void }) {
  const [codes, setCodes] = useState<CodeView[]>([]);
  const [loading, setLoading] = useState(true);
  const [quota, setQuota] = useState(30);
  const [count, setCount] = useState(1);
  const [customCode, setCustomCode] = useState("");
  const [note, setNote] = useState("");
  const [creating, setCreating] = useState(false);
  const [copied, setCopied] = useState("");

  const headers = useCallback(
    (json = false): HeadersInit =>
      json ? { "x-admin-password": pw, "Content-Type": "application/json" } : { "x-admin-password": pw },
    [pw],
  );

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch("/api/admin/codes", { headers: headers() });
      const json = (await res.json()) as { codes?: CodeView[] };
      setCodes(json.codes ?? []);
    } finally {
      setLoading(false);
    }
  }, [headers]);

  useEffect(() => {
    void load();
  }, [load]);

  async function create() {
    if (creating || quota < 1) return;
    setCreating(true);
    try {
      await fetch("/api/admin/codes", {
        method: "POST",
        headers: headers(true),
        body: JSON.stringify({
          quota,
          count,
          code: count === 1 && customCode.trim() ? customCode.trim() : undefined,
          note: note.trim() || undefined,
        }),
      });
      setCustomCode("");
      setNote("");
      await load();
    } finally {
      setCreating(false);
    }
  }

  async function toggle(c: CodeView) {
    await fetch(`/api/admin/codes/${c.id}`, {
      method: "PATCH",
      headers: headers(true),
      body: JSON.stringify({ enabled: !c.enabled }),
    });
    await load();
  }

  async function setQuotaFor(c: CodeView, q: number) {
    await fetch(`/api/admin/codes/${c.id}`, {
      method: "PATCH",
      headers: headers(true),
      body: JSON.stringify({ quota: q }),
    });
    await load();
  }

  async function remove(c: CodeView) {
    if (!window.confirm(`确定删除邀请码 ${c.code}？`)) return;
    await fetch(`/api/admin/codes/${c.id}`, { method: "DELETE", headers: headers() });
    await load();
  }

  function copy(code: string) {
    void navigator.clipboard.writeText(code);
    setCopied(code);
    setTimeout(() => setCopied(""), 1200);
  }

  const totalQuota = codes.reduce((a, c) => a + c.quota, 0);
  const totalUsed = codes.reduce((a, c) => a + c.used, 0);

  return (
    <div style={S.page}>
      <div style={S.header}>
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <ShieldCheck size={18} style={{ color: "var(--text)" }} />
          <span style={S.headerTitle}>邀请码管理</span>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button style={S.ghostBtn} onClick={() => void load()}>
            <RefreshCw size={14} /> 刷新
          </button>
          <button style={S.ghostBtn} onClick={onLogout}>
            <LogOut size={14} /> 退出
          </button>
        </div>
      </div>

      <div style={S.statsRow}>
        <Stat label="邀请码数量" value={codes.length} />
        <Stat label="总额度" value={totalQuota} />
        <Stat label="已使用" value={totalUsed} />
        <Stat label="剩余" value={totalQuota - totalUsed} />
      </div>

      <div style={S.createCard}>
        <span style={S.cardTitle}>新建邀请码</span>
        <div style={S.createRow}>
          <Labeled label="额度（次数）">
            <input type="number" min={1} value={quota} onChange={(e) => setQuota(Math.max(1, Number(e.target.value)))} style={S.smallInput} />
          </Labeled>
          <Labeled label="数量（批量）">
            <input type="number" min={1} max={100} value={count} onChange={(e) => setCount(Math.max(1, Math.min(100, Number(e.target.value))))} style={S.smallInput} />
          </Labeled>
          <Labeled label="自定义码（数量为 1 时可选）">
            <input
              value={customCode}
              onChange={(e) => setCustomCode(e.target.value.toUpperCase())}
              placeholder="留空则随机生成"
              disabled={count !== 1}
              style={{ ...S.textInput, opacity: count !== 1 ? 0.5 : 1 }}
            />
          </Labeled>
          <Labeled label="备注">
            <input value={note} onChange={(e) => setNote(e.target.value)} placeholder="可选" style={S.textInput} />
          </Labeled>
          <button style={S.primaryBtnSm} onClick={() => void create()} disabled={creating}>
            {creating ? <Loader2 size={14} style={{ animation: "spin 1s linear infinite" }} /> : <Plus size={14} />}
            生成
          </button>
        </div>
      </div>

      <div style={S.tableCard}>
        {loading ? (
          <div style={S.tableEmpty}>
            <Loader2 size={18} style={{ animation: "spin 1s linear infinite" }} /> 加载中…
          </div>
        ) : codes.length === 0 ? (
          <div style={S.tableEmpty}>还没有邀请码，先在上面创建一个。</div>
        ) : (
          <table style={S.table}>
            <thead>
              <tr>
                <th style={S.th}>邀请码</th>
                <th style={S.th}>额度</th>
                <th style={S.th}>已用</th>
                <th style={S.th}>剩余</th>
                <th style={S.th}>状态</th>
                <th style={{ ...S.th, textAlign: "right" }}>操作</th>
              </tr>
            </thead>
            <tbody>
              {codes.map((c) => (
                <tr key={c.code} style={S.tr}>
                  <td style={S.td}>
                    <span style={S.codeText}>{c.code}</span>
                    <button style={S.copyBtn} onClick={() => copy(c.code)} title="复制">
                      {copied === c.code ? <Check size={13} style={{ color: "#7ddb9a" }} /> : <Copy size={13} />}
                    </button>
                  </td>
                  <td style={S.td}>
                    <input
                      type="number"
                      min={c.used}
                      defaultValue={c.quota}
                      onBlur={(e) => {
                        const v = Number(e.target.value);
                        if (v !== c.quota) void setQuotaFor(c, v);
                      }}
                      style={S.quotaInput}
                    />
                  </td>
                  <td style={S.td}>{c.used}</td>
                  <td style={{ ...S.td, color: c.remaining > 0 ? "var(--text)" : "#ff8c8c" }}>{c.remaining}</td>
                  <td style={S.td}>
                    <span style={{ ...S.badge, ...(c.enabled ? S.badgeOn : S.badgeOff) }}>{c.enabled ? "启用" : "停用"}</span>
                  </td>
                  <td style={{ ...S.td, textAlign: "right" }}>
                    <button style={S.rowBtn} onClick={() => void toggle(c)} title={c.enabled ? "停用" : "启用"}>
                      <Power size={14} />
                    </button>
                    <button style={S.rowBtn} onClick={() => void remove(c)} title="删除">
                      <Trash2 size={14} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div style={S.stat}>
      <span style={S.statValue}>{value}</span>
      <span style={S.statLabel}>{label}</span>
    </div>
  );
}

function Labeled({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={S.labeled}>
      <span style={S.labeledText}>{label}</span>
      {children}
    </label>
  );
}

const S: Record<string, CSSProperties> = {
  center: { minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "var(--bg)" },
  loginCard: {
    width: 360,
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 18,
    boxShadow: "var(--shadow-panel)",
    padding: "32px 28px",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    textAlign: "center",
  },
  logo: {
    width: 48,
    height: 48,
    borderRadius: 14,
    background: "var(--bg-hover)",
    border: "1px solid var(--border)",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 16,
  },
  title: { fontSize: 18, fontWeight: 600, color: "var(--text)", margin: 0 },
  subtitle: { fontSize: 13, color: "var(--text-dim)", margin: "8px 0 20px" },
  input: {
    width: "100%",
    height: 44,
    padding: "0 14px",
    borderRadius: 11,
    border: "1px solid var(--border-strong)",
    background: "var(--bg-elevated)",
    color: "var(--text)",
    fontSize: 14,
    outline: "none",
  },
  error: { width: "100%", textAlign: "left", color: "#ff8c8c", fontSize: 12.5, marginTop: 8 },
  primaryBtn: {
    width: "100%",
    height: 44,
    marginTop: 16,
    borderRadius: 11,
    border: "none",
    background: "var(--btn-primary)",
    color: "#161618",
    fontSize: 14,
    fontWeight: 600,
    cursor: "pointer",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    boxShadow: "var(--shadow-btn)",
  },
  page: { minHeight: "100vh", background: "var(--bg)", padding: "32px 28px", maxWidth: 1040, margin: "0 auto" },
  header: { display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: 20 },
  headerTitle: { fontSize: 17, fontWeight: 600, color: "var(--text)" },
  ghostBtn: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    height: 34,
    padding: "0 12px",
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text-dim)",
    fontSize: 12.5,
    cursor: "pointer",
  },
  statsRow: { display: "grid", gridTemplateColumns: "repeat(4, 1fr)", gap: 12, marginBottom: 18 },
  stat: {
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 12,
    padding: "14px 16px",
    display: "flex",
    flexDirection: "column",
    gap: 4,
    boxShadow: "var(--shadow-soft)",
  },
  statValue: { fontSize: 22, fontWeight: 600, color: "var(--text)", fontVariantNumeric: "tabular-nums" },
  statLabel: { fontSize: 12, color: "var(--text-faint)" },
  createCard: {
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 14,
    padding: 18,
    marginBottom: 18,
    boxShadow: "var(--shadow-soft)",
  },
  cardTitle: { fontSize: 13.5, fontWeight: 600, color: "var(--text)" },
  createRow: { display: "flex", gap: 12, alignItems: "flex-end", flexWrap: "wrap", marginTop: 14 },
  labeled: { display: "flex", flexDirection: "column", gap: 6 },
  labeledText: { fontSize: 11.5, color: "var(--text-faint)" },
  smallInput: {
    width: 90,
    height: 38,
    padding: "0 12px",
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text)",
    fontSize: 13,
    outline: "none",
  },
  textInput: {
    width: 180,
    height: 38,
    padding: "0 12px",
    borderRadius: 9,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text)",
    fontSize: 13,
    outline: "none",
  },
  primaryBtnSm: {
    display: "inline-flex",
    alignItems: "center",
    gap: 6,
    height: 38,
    padding: "0 18px",
    borderRadius: 9,
    border: "none",
    background: "var(--btn-primary)",
    color: "#161618",
    fontSize: 13,
    fontWeight: 600,
    cursor: "pointer",
    boxShadow: "var(--shadow-btn)",
  },
  tableCard: {
    background: "var(--bg-panel)",
    border: "1px solid var(--border)",
    borderRadius: 14,
    overflow: "hidden",
    boxShadow: "var(--shadow-soft)",
  },
  tableEmpty: { padding: "40px 0", textAlign: "center", color: "var(--text-faint)", fontSize: 13, display: "flex", gap: 8, alignItems: "center", justifyContent: "center" },
  table: { width: "100%", borderCollapse: "collapse" },
  th: { textAlign: "left", fontSize: 11.5, fontWeight: 500, color: "var(--text-faint)", padding: "12px 16px", borderBottom: "1px solid var(--border)" },
  tr: { borderBottom: "1px solid var(--border)" },
  td: { fontSize: 13, color: "var(--text)", padding: "10px 16px", verticalAlign: "middle" },
  codeText: { fontFamily: "ui-monospace, monospace", letterSpacing: 1, fontSize: 13.5 },
  copyBtn: { marginLeft: 8, border: "none", background: "transparent", color: "var(--text-faint)", cursor: "pointer", verticalAlign: "middle" },
  quotaInput: {
    width: 70,
    height: 30,
    padding: "0 8px",
    borderRadius: 7,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text)",
    fontSize: 12.5,
    outline: "none",
  },
  badge: { fontSize: 11.5, padding: "2px 9px", borderRadius: 20, border: "1px solid var(--border)" },
  badgeOn: { color: "#7ddb9a", borderColor: "#2f4a39" },
  badgeOff: { color: "var(--text-faint)" },
  rowBtn: {
    width: 30,
    height: 30,
    marginLeft: 6,
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    borderRadius: 8,
    border: "1px solid var(--border)",
    background: "var(--bg-elevated)",
    color: "var(--text-dim)",
    cursor: "pointer",
  },
};
