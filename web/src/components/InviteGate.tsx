"use client";

import { useEffect, useState } from "react";
import {
  Box,
  Loader2,
  ArrowRight,
  Infinity as InfinityIcon,
  Scissors,
  Layers,
  Download,
} from "lucide-react";
import { initInvite, redeem, useInvite } from "@/lib/invite";

const FEATURES = [
  { icon: InfinityIcon, text: "无限画布 · 多图同屏依旧顺滑" },
  { icon: Layers, text: "一句话生成正 / 侧 / 顶 三视图" },
  { icon: Scissors, text: "自动裁切 + 手动微调分割线" },
  { icon: Download, text: "批量生成 · 一键打包导出" },
];

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
              <Box size={20} style={{ color: "var(--text)" }} />
            </span>
            <span className="gate__word">Canvora</span>
            <span className="gate__beta">CLOSED BETA</span>
          </div>

          <h1 className="gate__headline">
            把一句话，
            <br />
            变成可直接用的设计稿
          </h1>
          <p className="gate__sub">
            Canvora 是一块面向设计的无限 AI 画布。输入描述或参考图，即可生成可商用的多视图设计参考，并自动裁切、批量导出。
          </p>

          <div className="gate__features">
            {FEATURES.map((f) => (
              <div className="gate__feat" key={f.text}>
                <span className="gate__featIcon">
                  <f.icon size={15} />
                </span>
                {f.text}
              </div>
            ))}
          </div>

          <div className="gate__brandFoot">© {new Date().getFullYear()} Canvora · 内测版本，功能持续完善中</div>
        </section>

        <section className="gate__auth">
          <h2 className="gate__authTitle">内测访问</h2>
          <p className="gate__authSub">Canvora 当前处于邀请制内测阶段，请输入邀请码进入。</p>

          {ready ? (
            <>
              <label className="gate__label" htmlFor="gate-code">
                邀请码
              </label>
              <div className="gate__inputRow">
                <input
                  id="gate-code"
                  autoFocus
                  value={code}
                  onChange={(e) => {
                    setCode(e.target.value.toUpperCase());
                    setError("");
                  }}
                  onKeyDown={(e) => e.key === "Enter" && void submit()}
                  placeholder="请输入邀请码"
                  className="gate__input"
                  spellCheck={false}
                  autoComplete="off"
                />
              </div>
              <div className="gate__error">{error}</div>
              <button
                className="gate__btn"
                onClick={() => void submit()}
                disabled={busy || !code.trim()}
              >
                {busy ? (
                  <Loader2 size={17} style={{ animation: "spin 1s linear infinite" }} />
                ) : (
                  <>
                    进入工作台 <ArrowRight size={16} />
                  </>
                )}
              </button>
              <div className="gate__hint">
                还没有邀请码？内测名额有限，可联系管理员申请，或留意后续开放注册。
              </div>
            </>
          ) : (
            <div className="gate__loading">
              <Loader2 size={18} style={{ animation: "spin 1s linear infinite" }} />
              正在校验访问凭证…
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
