import requests
import base64
import json

API_KEY = "sk-1o2GpxQKS1pqWE19kOJlwls7Ssd0t5jU35NByNthSJenxK3w"
BASE_URL = "https://api.bltcy.ai"
IMAGE_PATH = r"D:\测试区域文件夹\app开发\ScreenShot_2026-05-22_203818_512.png"

PROMPT_CHIBI = """Scene:
Pure white background (#FFFFFF), no environment, no ground plane, no shadows.
Flat even lighting, no dramatic shadows.

Subject:
Three orthographic views of a CHIBI/Q-version (cute stylized) interpretation of the object from the input image, arranged HORIZONTALLY: Front (left), Side (center), Top (right).
Transform into adorable chibi proportions: large head (2-3 head ratio), rounded simplified body, cute exaggerated features.

Important details:
- Each view occupies EXACTLY one-third of the total image width with EQUAL white gaps.
- CHIBI proportions: oversized head, small rounded body, stubby limbs, big eyes.
- Smooth, clean surfaces with NO texture detail - like a vinyl toy or clay figure.
- Bold, clear silhouette outline - easily readable from any angle.
- Flat cel-shading style with minimal gradients (2-3 tone maximum).
- Round, soft edges everywhere - no sharp corners.
- Same scale and baseline alignment across all three views.
- Think: Nendoroid figure / Pop Mart blind box / vinyl designer toy.

Use case:
Cute 3D character model reference for 3D printing or figure production.

Constraints:
- NO background, NO ground, NO shadows on background, NO watermark.
- NO realistic proportions - must be CHIBI/deformed cute style.
- NO complex textures or patterns - keep surfaces SMOOTH and SIMPLE.
- NO text, NO labels, NO annotations.
- Do NOT deviate from equal-thirds horizontal layout.
- Maximum simplicity for clean 3D printability.
- Output image MUST be in 16:9 landscape aspect ratio (wider than tall)."""

print("Testing with red-hair girl image (Q版/Chibi style)...")
print(f"Input: {IMAGE_PATH}")

with open(IMAGE_PATH, "rb") as f:
    image_bytes = f.read()
print(f"Image size: {len(image_bytes)} bytes")

headers = {"Authorization": f"Bearer {API_KEY}"}
files = {"image": ("input.png", image_bytes, "image/png")}
data = {"prompt": PROMPT_CHIBI, "size": "1792x1024", "model": "gpt-image-1", "n": "1"}

print("Calling API...")
r = requests.post(f"{BASE_URL}/v1/images/edits", headers=headers, files=files, data=data, timeout=180)
print(f"Status: {r.status_code}")

if r.status_code == 200:
    result = r.json()
    if "data" in result and len(result["data"]) > 0:
        item = result["data"][0]
        if "url" in item:
            print(f"Generated image URL: {item['url']}")
            img_r = requests.get(item["url"], timeout=30)
            outpath = r"d:\测试区域文件夹\app开发\test_girl_chibi_output.png"
            with open(outpath, "wb") as f:
                f.write(img_r.content)
            print(f"Saved to: {outpath} ({len(img_r.content)} bytes)")
            
            # Analyze
            from PIL import Image
            import numpy as np
            img = Image.open(outpath)
            arr = np.array(img)
            h, w = arr.shape[:2]
            print(f"\nOutput analysis:")
            print(f"  Size: {w}x{h}")
            print(f"  Aspect ratio: {w/h:.2f} (target 1.78 for 16:9)")
            white_pixels = np.all(arr > 240, axis=2)
            print(f"  White background: {white_pixels.sum()/(h*w):.1%}")
            
            # Find gaps
            col_whiteness = np.mean(white_pixels, axis=0)
            gap_cols = col_whiteness > 0.9
            gaps = []
            in_gap = False
            start = 0
            for i in range(w):
                if gap_cols[i] and not in_gap:
                    start = i; in_gap = True
                elif not gap_cols[i] and in_gap:
                    if i - start > 15: gaps.append((start, i))
                    in_gap = False
            if in_gap and w - start > 15: gaps.append((start, w))
            print(f"  White gaps detected: {len(gaps)}")
            for s, e in gaps:
                print(f"    x={s}-{e} (width={e-s}px)")
        elif "b64_json" in item:
            out = base64.b64decode(item["b64_json"])
            outpath = r"d:\测试区域文件夹\app开发\test_girl_chibi_output.png"
            with open(outpath, "wb") as f:
                f.write(out)
            print(f"Saved (base64): {outpath} ({len(out)} bytes)")
    else:
        print(f"Unexpected response: {json.dumps(result, ensure_ascii=False)[:300]}")
else:
    print(f"Error: {r.text[:300]}")

print("\nDone!")
