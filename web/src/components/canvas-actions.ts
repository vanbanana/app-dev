import { AssetRecordType, createShapeId, type Editor, type TLShapeId } from "tldraw";
import { NODE_W, totalHeight, type ImageGenShape } from "./shapes/ImageGenShapeUtil";
import { BATCH_W, batchHeight, type BatchGenShape } from "./shapes/BatchGenShapeUtil";
import { getSettings } from "@/lib/settings";

/** Creates a new Image Generator node at the center of the current viewport. */
export function createImageGenNode(editor: Editor): TLShapeId {
  const id = createShapeId();
  const center = editor.getViewportPageBounds().center;
  const { defaultStyle, defaultRatio } = getSettings();
  const h = totalHeight(NODE_W, defaultRatio);
  editor.createShape<ImageGenShape>({
    id,
    type: "image-gen",
    x: center.x - NODE_W / 2,
    y: center.y - h / 2,
    props: { w: NODE_W, h, style: defaultStyle, ratio: defaultRatio, createdAt: Date.now() },
  });
  return id;
}

function readAsDataUrl(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

/**
 * Batch upload: stages the chosen images inside a single compact "batch" node
 * (no generation yet). The user reviews the count/thumbnails there and presses
 * "开始生成" to spawn the result nodes and run them through the shared queue.
 */
export async function createBatchNodeFromFiles(editor: Editor, files: File[]): Promise<void> {
  const { defaultStyle, defaultRatio } = getSettings();
  const images = await Promise.all(files.map((f) => readAsDataUrl(f)));
  const h = batchHeight(images.length);
  const center = editor.getViewportPageBounds().center;

  const id = createShapeId();
  editor.createShape<BatchGenShape>({
    id,
    type: "batch-gen",
    x: center.x - BATCH_W / 2,
    y: center.y - h / 2,
    props: {
      w: BATCH_W,
      h,
      images,
      style: defaultStyle,
      ratio: defaultRatio,
      started: false,
      childIds: [],
      createdAt: Date.now(),
    },
  });
  editor.select(id);
  editor.zoomToSelection({ animation: { duration: 200 } });
}

function loadImageSize(src: string): Promise<{ w: number; h: number }> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve({ w: img.naturalWidth, h: img.naturalHeight });
    img.onerror = reject;
    img.src = src;
  });
}

/** Reads an image file and places it on the canvas centered in the viewport. */
export async function addImageFromFile(editor: Editor, file: File): Promise<void> {
  const dataUrl = await new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => resolve(String(reader.result));
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });

  const { w, h } = await loadImageSize(dataUrl);
  const maxSide = 520;
  const scale = Math.min(1, maxSide / Math.max(w, h));
  const dw = Math.round(w * scale);
  const dh = Math.round(h * scale);

  const assetId = AssetRecordType.createId();
  editor.createAssets([
    {
      id: assetId,
      type: "image",
      typeName: "asset",
      props: {
        name: file.name,
        src: dataUrl,
        w,
        h,
        mimeType: file.type || "image/png",
        isAnimated: false,
      },
      meta: {},
    },
  ]);

  const center = editor.getViewportPageBounds().center;
  const id = createShapeId();
  editor.createShape({
    id,
    type: "image",
    x: center.x - dw / 2,
    y: center.y - dh / 2,
    props: { assetId, w: dw, h: dh },
  });
  editor.select(id);
}
