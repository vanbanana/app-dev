import { AssetRecordType, createShapeId, type Editor, type TLShapeId } from "tldraw";
import { NODE_W, totalHeight, type ImageGenShape } from "./shapes/ImageGenShapeUtil";
import { getSettings } from "@/lib/settings";
import { runGeneration } from "@/lib/generation";

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
 * Batch upload: creates one Image Generator node per image (laid out in a grid),
 * each pre-filled with the image as reference, then kicks off generation for all
 * of them through the shared concurrency queue.
 */
export async function createImageGenNodesFromFiles(editor: Editor, files: File[]): Promise<void> {
  const { defaultStyle, defaultRatio } = getSettings();
  const h = totalHeight(NODE_W, defaultRatio);
  const gapX = NODE_W + 60;
  const gapY = h + 60;
  const cols = Math.ceil(Math.sqrt(files.length));
  const origin = editor.getViewportPageBounds().center;
  const startX = origin.x - ((Math.min(cols, files.length) - 1) * gapX) / 2 - NODE_W / 2;
  const startY = origin.y - h / 2;

  const ids: TLShapeId[] = [];
  for (let i = 0; i < files.length; i++) {
    const dataUrl = await readAsDataUrl(files[i]);
    const id = createShapeId();
    const col = i % cols;
    const row = Math.floor(i / cols);
    editor.createShape<ImageGenShape>({
      id,
      type: "image-gen",
      x: startX + col * gapX,
      y: startY + row * gapY,
      props: {
        w: NODE_W,
        h,
        style: defaultStyle,
        ratio: defaultRatio,
        referenceImage: dataUrl,
        createdAt: Date.now(),
      },
    });
    ids.push(id);
  }

  editor.select(...ids);
  editor.zoomToSelection({ animation: { duration: 200 } });
  ids.forEach((id) => void runGeneration(editor, id));
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
