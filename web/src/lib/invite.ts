"use client";

import { useSyncExternalStore } from "react";

export type Invite = {
  code: string;
  quota: number;
  used: number;
  remaining: number;
  enabled: boolean;
};

export type InviteState = { invite: Invite | null; ready: boolean };

const STORAGE_KEY = "atelier.invite.code";

let state: InviteState = { invite: null, ready: false };
const listeners = new Set<() => void>();
const SERVER_STATE: InviteState = { invite: null, ready: false };

function emit() {
  state = { ...state };
  listeners.forEach((l) => l());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getInviteState(): InviteState {
  return state;
}

export function getCode(): string | null {
  return state.invite?.code ?? null;
}

export function applyInvite(invite: Invite): void {
  state.invite = invite;
  state.ready = true;
  if (typeof window !== "undefined") localStorage.setItem(STORAGE_KEY, invite.code);
  emit();
}

export function clearInvite(): void {
  state.invite = null;
  state.ready = true;
  if (typeof window !== "undefined") localStorage.removeItem(STORAGE_KEY);
  emit();
}

/** Validate any stored code on first load. */
export async function initInvite(): Promise<void> {
  if (typeof window === "undefined") return;
  const stored = localStorage.getItem(STORAGE_KEY);
  if (!stored) {
    state.ready = true;
    emit();
    return;
  }
  try {
    const res = await fetch(`/api/me?code=${encodeURIComponent(stored)}`);
    if (res.ok) {
      const json = (await res.json()) as { invite: Invite };
      if (json.invite.enabled) {
        applyInvite(json.invite);
        return;
      }
    }
  } catch {
    // fall through to cleared state
  }
  clearInvite();
}

export async function redeem(code: string): Promise<{ ok: boolean; error?: string }> {
  try {
    const res = await fetch("/api/redeem", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code }),
    });
    const json = (await res.json()) as { invite?: Invite; error?: string };
    if (!res.ok || !json.invite) return { ok: false, error: json.error ?? "邀请码无效" };
    applyInvite(json.invite);
    return { ok: true };
  } catch {
    return { ok: false, error: "网络错误，请重试" };
  }
}

export async function refreshInvite(): Promise<void> {
  const code = getCode();
  if (!code) return;
  try {
    const res = await fetch(`/api/me?code=${encodeURIComponent(code)}`);
    if (res.ok) {
      const json = (await res.json()) as { invite: Invite };
      applyInvite(json.invite);
    }
  } catch {
    // ignore
  }
}

export function useInvite(): InviteState {
  return useSyncExternalStore(
    subscribe,
    getInviteState,
    () => SERVER_STATE,
  );
}
