"use client";

import { useSyncExternalStore } from "react";

export type PanelId = "history" | "settings" | null;

let openPanel: PanelId = null;
const listeners = new Set<() => void>();

function emit() {
  listeners.forEach((l) => l());
}

export function setOpenPanel(panel: PanelId): void {
  openPanel = openPanel === panel ? null : panel;
  emit();
}

export function closePanel(): void {
  openPanel = null;
  emit();
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useOpenPanel(): PanelId {
  return useSyncExternalStore(
    subscribe,
    () => openPanel,
    () => null,
  );
}
