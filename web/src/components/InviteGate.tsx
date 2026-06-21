"use client";

import { useEffect, useState, type CSSProperties } from "react";
import { Box, Loader2, KeyRound, ArrowRight } from "lucide-react";
import { initInvite, redeem, useInvite } from "@/lib/invite";

export function InviteGate() {
  const { invite, ready } = useInvite();
  const [code, setCode] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void initInvite();
  }, []);

  if (invite) return null;

  async function submit() {
    const trimmed = code.trim();
    if (!trimmed || busy) return;
    setBusy(true);
    setError("");
    const result = await redeem(trimmed);
    setBusy(false);
    if (!result.ok) setError(result.error ?? "邀请码无效");
  }

  return (
    <div style={S.overlay}>
      <div style={S.card}>
        <div style={S.logo}>
          <Box size={22} style={{ color: "var(--text)" }} />
        </div>
        <h1 style={S.title}>Atelier · 三视图工作台</h1>
        <p style={S.subtitle}>输入邀请码即可开始使用，每个邀请码有固定的可用次数。</p>

        {ready ? (
          <>
            <div style={S.inputRow}>
              <KeyRound size={16} style={{ color: "var(--text-faint)", flexShrink: 0 }} />
              <input
                autoFocus
                value={code}
                onChange={(e) => {
                  setCode(e.target.value.toUpperCase());
                  setError("");
                }}
                onKeyDown={(e) => e.key === "Enter" && void submit()}
                placeholder="请输入邀请码"
                style={S.input}
                spellCheck={false}
              />
            </div>
            {error && <div style={S.error}>{error}</div>}
            <button style={{ ...S.button, opacity: busy || !code.trim() ? 0.6 : 1 }} onClick={() => void submit()} disabled={busy || !code.trim()}>
              {busy ? <Loader2 size={16} style={{ animation: "spin 1s linear infinite" }} /> : <>进入 <ArrowRight size={15} /></>}
            </button>
          </>
        ) : (
          <div style={S.loading}>
            <Loader2 size={18} style={{ animation: "spin 1s linear infinite" }} />
            <span>正在校验…</span>
          </div>
        )}
      </div>
    </div>
  );
}

const S: Record<string, CSSProperties> = {
  overlay: {
    position: "fixed",
    inset: 0,
    zIndex: 1000,
    background: "var(--bg)",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  card: {
    width: 380,
    maxWidth: "calc(100vw - 40px)",
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
  subtitle: { fontSize: 13, color: "var(--text-dim)", lineHeight: 1.6, margin: "10px 0 22px" },
  inputRow: {
    width: "100%",
    display: "flex",
    alignItems: "center",
    gap: 8,
    height: 46,
    padding: "0 14px",
    borderRadius: 11,
    border: "1px solid var(--border-strong)",
    background: "var(--bg-elevated)",
  },
  input: {
    flex: 1,
    background: "transparent",
    border: "none",
    outline: "none",
    color: "var(--text)",
    fontSize: 15,
    letterSpacing: 2,
    fontVariantNumeric: "tabular-nums",
  },
  error: { width: "100%", textAlign: "left", color: "#ff8c8c", fontSize: 12.5, marginTop: 8 },
  button: {
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
    gap: 6,
    boxShadow: "var(--shadow-btn)",
  },
  loading: { display: "flex", alignItems: "center", gap: 8, color: "var(--text-dim)", fontSize: 13, marginTop: 8 },
};
