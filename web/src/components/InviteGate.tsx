"use client";

/* eslint-disable @next/next/no-img-element */
import { useEffect, useState } from "react";
import { Box, Loader2, ArrowRight } from "lucide-react";
import { initInvite, redeem, useInvite } from "@/lib/invite";

const SHOTS_A = ["/showcase/1.png", "/showcase/4.png"];
const SHOTS_B = ["/showcase/2.png", "/showcase/1.png"];

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
    <div className="gate">
      <div className="gate__bg" />
      <div className="gate__frame">
        <section className="gate__brand">
          <div className="gate__brandTop">
            <span className="gate__mark">
              <Box size={19} style={{ color: "var(--text)" }} />
            </span>
            <span className="gate__word">Canvora</span>
            <span className="gate__beta">CLOSED BETA</span>
          </div>

          <div className="gate__hero">
            <h1 className="gate__headline">
              一句话，
              <br />
              生成可用的<span>三视图</span>
            </h1>

            {ready ? (
              <>
                <div className="gate__inputRow">
                  <input
                    autoFocus
                    value={code}
                    onChange={(e) => {
                      setCode(e.target.value.toUpperCase());
                      setError("");
                    }}
                    onKeyDown={(e) => e.key === "Enter" && void submit()}
                    placeholder="输入邀请码"
                    className="gate__input"
                    spellCheck={false}
                    autoComplete="off"
                    aria-label="邀请码"
                  />
                  <button
                    className="gate__submit"
                    onClick={() => void submit()}
                    disabled={busy || !code.trim()}
                    aria-label="进入"
                  >
                    {busy ? (
                      <Loader2 size={17} style={{ animation: "spin 1s linear infinite" }} />
                    ) : (
                      <ArrowRight size={18} />
                    )}
                  </button>
                </div>
                <div className="gate__error">{error}</div>
                <div className="gate__foot">邀请制内测 · 名额有限</div>
              </>
            ) : (
              <div className="gate__loading">
                <Loader2 size={18} style={{ animation: "spin 1s linear infinite" }} />
                正在校验…
              </div>
            )}
          </div>
        </section>

        <aside className="gate__show" aria-hidden>
          <div className="gate__track">
            <div className="gate__col">
              {[...SHOTS_A, ...SHOTS_A].map((src, i) => (
                <div className="gate__shot" key={`a${i}`}>
                  <img src={src} alt="" draggable={false} />
                </div>
              ))}
            </div>
            <div className="gate__col gate__col--b">
              {[...SHOTS_B, ...SHOTS_B].map((src, i) => (
                <div className="gate__shot" key={`b${i}`}>
                  <img src={src} alt="" draggable={false} />
                </div>
              ))}
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
