"use client";

import { useSyncExternalStore } from "react";
import type { ImageGenStyle } from "@/components/shapes/ImageGenShapeUtil";

export type AppSettings = {
  /** Max number of generations running at the same time. */
  concurrency: number;
  /** Delay between dequeuing successive queued generations (ms). */
  scheduleDelay: number;
  /** Whether to auto-crop the three-view result into front/side/top. */
  autoCrop: boolean;
  /** Default style for new nodes. */
  defaultStyle: ImageGenStyle;
  /** Default aspect ratio for new nodes. */
  defaultRatio: string;
};

export const DEFAULT_SETTINGS: AppSettings = {
  concurrency: 3,
  scheduleDelay: 400,
  autoCrop: true,
  defaultStyle: "realistic",
  defaultRatio: "3:2",
};

const KEY = "atelier.settings.v1";

let current: AppSettings = DEFAULT_SETTINGS;
const listeners = new Set<() => void>();

function load(): AppSettings {
  if (typeof window === "undefined") return DEFAULT_SETTINGS;
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return DEFAULT_SETTINGS;
    return { ...DEFAULT_SETTINGS, ...(JSON.parse(raw) as Partial<AppSettings>) };
  } catch {
    return DEFAULT_SETTINGS;
  }
}

if (typeof window !== "undefined") {
  current = load();
}

export function getSettings(): AppSettings {
  return current;
}

export function updateSettings(patch: Partial<AppSettings>): void {
  current = { ...current, ...patch };
  if (typeof window !== "undefined") {
    try {
      window.localStorage.setItem(KEY, JSON.stringify(current));
    } catch {
      /* ignore quota errors */
    }
  }
  listeners.forEach((l) => l());
}

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function useSettings(): AppSettings {
  return useSyncExternalStore(subscribe, getSettings, () => DEFAULT_SETTINGS);
}
