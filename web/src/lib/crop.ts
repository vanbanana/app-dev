// Browser port of the original Android `DefaultCropperService`.
// Splits a horizontal three-view sheet (front / side / top on white) into
// three tight crops by detecting non-white content columns.

const CONTENT_THRESHOLD = 250; // grayscale below this = "content"
const CROP_PADDING = 6; // px padding around each detected region

export const VIEW_LABELS = ["front", "side", "top"] as const;
export const VIEW_LABELS_CN = ["正面图", "侧面图", "俯视图"] as const;

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "anonymous";
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = src;
  });
}

type Region = { startX: number; endX: number };

function findContentColumns(data: Uint8ClampedArray, w: number, h: number): number[] {
  const cols: number[] = [];
  for (let x = 0; x < w; x++) {
    for (let y = 0; y < h; y++) {
      const i = (y * w + x) * 4;
      const a = data[i + 3];
      const gray = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
      // transparent pixels count as background
      if (a > 10 && gray < CONTENT_THRESHOLD) {
        cols.push(x);
        break;
      }
    }
  }
  return cols;
}

function groupConsecutive(cols: number[]): Region[] {
  if (cols.length === 0) return [];
  const regions: Region[] = [];
  let start = cols[0];
  let end = cols[0];
  for (let i = 1; i < cols.length; i++) {
    if (cols[i] <= end + 3) {
      end = cols[i];
    } else {
      regions.push({ startX: start, endX: end });
      start = cols[i];
      end = cols[i];
    }
  }
  regions.push({ startX: start, endX: end });
  return regions;
}

/**
 * Detects the two vertical split positions (as fractions 0..1) that separate
 * the three views. Returns equal-thirds if detection is inconclusive.
 */
export async function detectSplits(src: string): Promise<number[]> {
  const img = await loadImage(src);
  const w = img.naturalWidth;
  const h = img.naturalHeight;
  if (w < 30 || h < 10) return [1 / 3, 2 / 3];

  const canvas = document.createElement("canvas");
  canvas.width = w;
  canvas.height = h;
  const ctx = canvas.getContext("2d", { willReadFrequently: true });
  if (!ctx) return [1 / 3, 2 / 3];
  ctx.drawImage(img, 0, 0);
  const { data } = ctx.getImageData(0, 0, w, h);

  const regions = groupConsecutive(findContentColumns(data, w, h));
  if (regions.length < 3) return [1 / 3, 2 / 3];

  const top3 = regions
    .slice()
    .sort((a, b) => b.endX - b.startX - (a.endX - a.startX))
    .slice(0, 3)
    .sort((a, b) => a.startX - b.startX);

  const s1 = (top3[0].endX + top3[1].startX) / 2 / w;
  const s2 = (top3[1].endX + top3[2].startX) / 2 / w;
  return [s1, s2];
}

/**
 * Crops the three views from the source image given two split fractions.
 * Each slice is tightened to its content bounding box with a little padding.
 * Returns three PNG data URLs (front, side, top).
 */
export async function cropViews(src: string, splits: number[]): Promise<string[]> {
  const img = await loadImage(src);
  const w = img.naturalWidth;
  const h = img.naturalHeight;
  const [s1, s2] = splits.length === 2 ? splits : [1 / 3, 2 / 3];

  const base = document.createElement("canvas");
  base.width = w;
  base.height = h;
  const bctx = base.getContext("2d", { willReadFrequently: true });
  if (!bctx) throw new Error("Canvas unavailable");
  bctx.drawImage(img, 0, 0);
  const { data } = bctx.getImageData(0, 0, w, h);

  const ranges: [number, number][] = [
    [0, Math.round(s1 * w)],
    [Math.round(s1 * w), Math.round(s2 * w)],
    [Math.round(s2 * w), w],
  ];

  return ranges.map(([x0, x1]) => {
    const sliceW = Math.max(1, x1 - x0);
    // tight content bbox within [x0, x1)
    let minX = x1;
    let maxX = x0;
    let minY = h;
    let maxY = 0;
    let found = false;
    for (let x = x0; x < x1; x++) {
      for (let y = 0; y < h; y++) {
        const i = (y * w + x) * 4;
        const a = data[i + 3];
        const gray = 0.299 * data[i] + 0.587 * data[i + 1] + 0.114 * data[i + 2];
        if (a > 10 && gray < CONTENT_THRESHOLD) {
          if (x < minX) minX = x;
          if (x > maxX) maxX = x;
          if (y < minY) minY = y;
          if (y > maxY) maxY = y;
          found = true;
        }
      }
    }
    if (!found) {
      minX = x0;
      maxX = x1 - 1;
      minY = 0;
      maxY = h - 1;
    }
    const cl = Math.max(0, minX - CROP_PADDING);
    const cr = Math.min(w - 1, maxX + CROP_PADDING);
    const ct = Math.max(0, minY - CROP_PADDING);
    const cb = Math.min(h - 1, maxY + CROP_PADDING);
    const cw = Math.max(1, Math.min(sliceW + 2 * CROP_PADDING, cr - cl + 1));
    const ch = Math.max(1, cb - ct + 1);

    const out = document.createElement("canvas");
    out.width = cw;
    out.height = ch;
    const octx = out.getContext("2d");
    if (!octx) throw new Error("Canvas unavailable");
    octx.fillStyle = "#ffffff";
    octx.fillRect(0, 0, cw, ch);
    octx.drawImage(base, cl, ct, cw, ch, 0, 0, cw, ch);
    return out.toDataURL("image/png");
  });
}

export function dataUrlToBlob(dataUrl: string): Blob {
  const [head, b64] = dataUrl.split(",");
  const mime = /data:(.*?);/.exec(head)?.[1] ?? "image/png";
  const bin = atob(b64);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return new Blob([bytes], { type: mime });
}
