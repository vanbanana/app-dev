"use client";

import {
  BaseBoxShapeUtil,
  HTMLContainer,
  T,
  stopEventPropagation,
  type RecordProps,
  type TLBaseShape,
} from "tldraw";
import { ImageGenNode } from "./ImageGenNode";

export type ImageGenStyle = "realistic" | "chibi";
export type ImageGenStatus = "idle" | "queued" | "generating" | "done" | "error";

export type ImageGenShape = TLBaseShape<
  "image-gen",
  {
    w: number;
    h: number;
    prompt: string;
    style: ImageGenStyle;
    ratio: string;
    status: ImageGenStatus;
    imageUrl: string;
    referenceImage: string;
    error: string;
    /** two split fractions (0..1) separating front/side/top; empty = uncropped */
    splits: number[];
    createdAt: number;
  }
>;

export const NODE_W = 460;
export const TITLE_H = 22;
export const GAP_1 = 8;
export const GAP_2 = 14;
export const PANEL_H = 182;
export const CROPS_H = 104;

export function ratioParts(ratio: string): [number, number] {
  const [a, b] = ratio.split(":").map((n) => Number(n));
  if (!a || !b) return [3, 2];
  return [a, b];
}

export function frameHeight(width: number, ratio: string): number {
  const [w, h] = ratioParts(ratio);
  return Math.round((width * h) / w);
}

export function totalHeight(width: number, ratio: string, hasCrops = false): number {
  return (
    TITLE_H +
    GAP_1 +
    frameHeight(width, ratio) +
    (hasCrops ? CROPS_H : 0) +
    GAP_2 +
    PANEL_H
  );
}

export function ratioToSize(ratio: string): string {
  switch (ratio) {
    case "2:3":
      return "1024x1536";
    case "1:1":
      return "1024x1024";
    case "16:9":
    case "3:2":
    default:
      return "1536x1024";
  }
}

export class ImageGenShapeUtil extends BaseBoxShapeUtil<ImageGenShape> {
  static override type = "image-gen" as const;

  static override props: RecordProps<ImageGenShape> = {
    w: T.number,
    h: T.number,
    prompt: T.string,
    style: T.literalEnum("realistic", "chibi"),
    ratio: T.string,
    status: T.literalEnum("idle", "queued", "generating", "done", "error"),
    imageUrl: T.string,
    referenceImage: T.string,
    error: T.string,
    splits: T.arrayOf(T.number),
    createdAt: T.number,
  };

  override getDefaultProps(): ImageGenShape["props"] {
    return {
      w: NODE_W,
      h: totalHeight(NODE_W, "3:2"),
      prompt: "",
      style: "realistic",
      ratio: "3:2",
      status: "idle",
      imageUrl: "",
      referenceImage: "",
      error: "",
      splits: [],
      createdAt: Date.now(),
    };
  }

  override canResize() {
    return false;
  }

  override canEdit() {
    return false;
  }

  override hideRotateHandle() {
    return true;
  }

  override onResize(shape: ImageGenShape) {
    return shape;
  }

  override component(shape: ImageGenShape) {
    return (
      <HTMLContainer
        style={{
          width: shape.props.w,
          height: shape.props.h,
          pointerEvents: "all",
        }}
        onPointerDown={(e) => {
          // Allow tldraw to start a drag from the title/frame, but never let a
          // stray pointerdown bubble into text selection inside controls.
          if ((e.target as HTMLElement).closest("[data-interactive]")) {
            stopEventPropagation(e);
          }
        }}
      >
        <ImageGenNode shape={shape} editor={this.editor} />
      </HTMLContainer>
    );
  }

  override indicator(shape: ImageGenShape) {
    return <rect width={shape.props.w} height={shape.props.h} rx={16} ry={16} />;
  }
}
