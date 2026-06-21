import JSZip from "jszip";
import { cropViews, dataUrlToBlob, VIEW_LABELS } from "./crop";

function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

export function downloadDataUrl(dataUrl: string, filename: string): void {
  triggerDownload(dataUrlToBlob(dataUrl), filename);
}

/** Downloads a single cropped view (0=front, 1=side, 2=top). */
export async function downloadSingleView(
  imageUrl: string,
  splits: number[],
  index: number,
  baseName: string,
): Promise<void> {
  const crops = await cropViews(imageUrl, splits);
  const label = VIEW_LABELS[index] ?? `view_${index}`;
  downloadDataUrl(crops[index], `${baseName}_${label}.png`);
}

/** Downloads all three cropped views as separate PNG files. */
export async function downloadAllViews(
  imageUrl: string,
  splits: number[],
  baseName: string,
): Promise<void> {
  const crops = await cropViews(imageUrl, splits);
  crops.forEach((dataUrl, i) => {
    downloadDataUrl(dataUrl, `${baseName}_${VIEW_LABELS[i]}.png`);
  });
}

/** Packages the three cropped views (+ the full sheet) into a single ZIP. */
export async function downloadViewsZip(
  imageUrl: string,
  splits: number[],
  baseName: string,
): Promise<void> {
  const crops = await cropViews(imageUrl, splits);
  const zip = new JSZip();
  const folder = zip.folder(baseName) ?? zip;
  folder.file("three_view.png", dataUrlToBlob(imageUrl));
  crops.forEach((dataUrl, i) => {
    folder.file(`${VIEW_LABELS[i]}.png`, dataUrlToBlob(dataUrl));
  });
  const blob = await zip.generateAsync({ type: "blob" });
  triggerDownload(blob, `${baseName}.zip`);
}

/** Packages many tasks into one ZIP, each in its own subfolder. */
export async function downloadBatchZip(
  tasks: { imageUrl: string; splits: number[]; name: string }[],
): Promise<void> {
  const zip = new JSZip();
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  const root = zip.folder(`canvora_batch_${date}`) ?? zip;
  for (let t = 0; t < tasks.length; t++) {
    const task = tasks[t];
    const crops = await cropViews(task.imageUrl, task.splits);
    const folder = root.folder(`task${t + 1}_${task.name}`) ?? root;
    folder.file("three_view.png", dataUrlToBlob(task.imageUrl));
    crops.forEach((dataUrl, i) => {
      folder.file(`${VIEW_LABELS[i]}.png`, dataUrlToBlob(dataUrl));
    });
  }
  const blob = await zip.generateAsync({ type: "blob" });
  triggerDownload(blob, `canvora_batch_${date}_${tasks.length}tasks.zip`);
}

export function canShareFiles(): boolean {
  return (
    typeof navigator !== "undefined" &&
    typeof navigator.canShare === "function" &&
    typeof navigator.share === "function"
  );
}

/** Shares the full three-view sheet via the Web Share API (with download fallback). */
export async function shareImage(imageUrl: string, baseName: string): Promise<void> {
  const blob = dataUrlToBlob(imageUrl);
  const file = new File([blob], `${baseName}.png`, { type: blob.type });
  if (canShareFiles() && navigator.canShare({ files: [file] })) {
    await navigator.share({ files: [file], title: baseName });
  } else {
    triggerDownload(blob, `${baseName}.png`);
  }
}

/** Shares the views ZIP via the Web Share API (with download fallback). */
export async function shareZip(
  imageUrl: string,
  splits: number[],
  baseName: string,
): Promise<void> {
  const crops = await cropViews(imageUrl, splits);
  const zip = new JSZip();
  const folder = zip.folder(baseName) ?? zip;
  folder.file("three_view.png", dataUrlToBlob(imageUrl));
  crops.forEach((dataUrl, i) => folder.file(`${VIEW_LABELS[i]}.png`, dataUrlToBlob(dataUrl)));
  const blob = await zip.generateAsync({ type: "blob" });
  const file = new File([blob], `${baseName}.zip`, { type: "application/zip" });
  if (canShareFiles() && navigator.canShare({ files: [file] })) {
    await navigator.share({ files: [file], title: baseName });
  } else {
    triggerDownload(blob, `${baseName}.zip`);
  }
}
