"use client";

import {
  BaseBoxShapeUtil,
  HTMLContainer,
  T,
  stopEventPropagation,
  type RecordProps,
  type TLBaseShape,
} from "tldraw";
import { BatchGenNode } from "./BatchGenNode";
import type { ImageGenStyle } from "./ImageGenShapeUtil";

export type BatchGenShape = TLBaseShape<
  "batch-gen",
  {
    w: number;
    h: number;
    images: string[]; // data URLs of uploaded reference images
    style: ImageGenStyle;
    ratio: string;
    started: boolean;
    childIds: string[]; // ids of the spawned result nodes (1:1 with images)
    createdAt: number;
  }
>;

export const BATCH_W = 412;

const PAD = 18;
const HEADER_H = 30;
const HEADER_GAP = 14;
const THUMB = 84;
const THUMB_GAP = 8;
const COLS = 4;
const MAX_CELLS = 8; // up to 7 thumbs + 1 overflow chip
const CONTROLS_GAP = 14;
const CONTROLS_H = 42;
const PROGRESS_H = 22;

/** Number of thumbnail cells shown (capped, last becomes a "+N" chip). */
export function visibleCells(count: number): number {
  return Math.min(count, MAX_CELLS);
}

export function batchHeight(count: number): number {
  const cells = Math.max(1, visibleCells(count));
  const rows = Math.ceil(cells / COLS);
  const gridH = rows * THUMB + (rows - 1) * THUMB_GAP;
  return (
    PAD +
    HEADER_H +
    HEADER_GAP +
    gridH +
    PROGRESS_H +
    CONTROLS_GAP +
    CONTROLS_H +
    PAD
  );
}

export const BATCH_LAYOUT = { PAD, THUMB, THUMB_GAP, COLS, MAX_CELLS };

export class BatchGenShapeUtil extends BaseBoxShapeUtil<BatchGenShape> {
  static override type = "batch-gen" as const;

  static override props: RecordProps<BatchGenShape> = {
    w: T.number,
    h: T.number,
    images: T.arrayOf(T.string),
    style: T.literalEnum("realistic", "chibi"),
    ratio: T.string,
    started: T.boolean,
    childIds: T.arrayOf(T.string),
    createdAt: T.number,
  };

  override getDefaultProps(): BatchGenShape["props"] {
    return {
      w: BATCH_W,
      h: batchHeight(0),
      images: [],
      style: "realistic",
      ratio: "3:2",
      started: false,
      childIds: [],
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

  override onResize(shape: BatchGenShape) {
    return shape;
  }

  override component(shape: BatchGenShape) {
    return (
      <HTMLContainer
        style={{ width: shape.props.w, height: shape.props.h, pointerEvents: "all" }}
        onPointerDown={(e) => {
          if ((e.target as HTMLElement).closest("[data-interactive]")) {
            stopEventPropagation(e);
          }
        }}
      >
        <BatchGenNode shape={shape} editor={this.editor} />
      </HTMLContainer>
    );
  }

  override indicator(shape: BatchGenShape) {
    return <rect width={shape.props.w} height={shape.props.h} rx={18} ry={18} />;
  }
}
