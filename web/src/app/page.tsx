"use client";

import dynamic from "next/dynamic";
import { InviteGate } from "@/components/InviteGate";

const Canvas = dynamic(() => import("@/components/Canvas"), {
  ssr: false,
  loading: () => (
    <div
      style={{
        position: "fixed",
        inset: 0,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        color: "var(--text-faint)",
        fontSize: 14,
      }}
    >
      加载画布…
    </div>
  ),
});

export default function Home() {
  return (
    <>
      <Canvas />
      <InviteGate />
    </>
  );
}
