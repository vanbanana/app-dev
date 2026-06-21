import requests
import base64
import json
import os

API_KEY = "sk-1o2GpxQKS1pqWE19kOJlwls7Ssd0t5jU35NByNthSJenxK3w"
BASE_URL = "https://api.bltcy.ai"
IMAGE_PATH = r"d:\测试区域文件夹\app开发\assets\slice_1.png"

PROMPT = """Scene:
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

print("=" * 60)
print("Testing API: Three-View Generation")
print("=" * 60)

# Read image
with open(IMAGE_PATH, "rb") as f:
    image_bytes = f.read()
print(f"Input image: {IMAGE_PATH} ({len(image_bytes)} bytes)")

headers = {"Authorization": f"Bearer {API_KEY}"}

# First, try to list available models
print("\n--- Checking available models ---")
try:
    r = requests.get(f"{BASE_URL}/v1/models", headers=headers, timeout=15)
    print(f"GET /v1/models -> {r.status_code}")
    if r.status_code == 200:
        models = r.json()
        if "data" in models:
            for m in models["data"][:10]:
                print(f"  - {m.get('id', 'unknown')}")
        else:
            print(f"  Response: {str(models)[:300]}")
    else:
        print(f"  Error: {r.text[:200]}")
except Exception as e:
    print(f"  Failed: {e}")

# Try /v1/images/edits (OpenAI standard for image editing)
print("\n--- Try 1: POST /v1/images/edits ---")
try:
    files = {"image": ("input.png", image_bytes, "image/png")}
    data = {"prompt": PROMPT, "size": "1792x1024", "model": "gpt-image-1", "n": "1"}
    r = requests.post(f"{BASE_URL}/v1/images/edits", headers=headers, files=files, data=data, timeout=120)
    print(f"Status: {r.status_code}")
    if r.status_code == 200:
        result = r.json()
        print("SUCCESS!")
        if "data" in result:
            for i, item in enumerate(result["data"]):
                if "b64_json" in item:
                    out = base64.b64decode(item["b64_json"])
                    outpath = r"d:\测试区域文件夹\app开发\test_output.png"
                    with open(outpath, "wb") as f:
                        f.write(out)
                    print(f"Saved to: {outpath} ({len(out)} bytes)")
                elif "url" in item:
                    print(f"URL: {item['url'][:100]}")
                    img_r = requests.get(item["url"], timeout=30)
                    outpath = r"d:\测试区域文件夹\app开发\test_output.png"
                    with open(outpath, "wb") as f:
                        f.write(img_r.content)
                    print(f"Downloaded to: {outpath} ({len(img_r.content)} bytes)")
    else:
        print(f"Error: {r.text[:300]}")
except Exception as e:
    print(f"Failed: {e}")

# Try /v1/images/generations with base64 image in prompt
print("\n--- Try 2: POST /v1/images/generations ---")
try:
    img_b64 = base64.b64encode(image_bytes).decode()
    json_data = {
        "model": "gpt-image-1",
        "prompt": PROMPT,
        "size": "1792x1024",
        "quality": "high",
        "n": 1,
    }
    r = requests.post(f"{BASE_URL}/v1/images/generations", headers=headers, json=json_data, timeout=120)
    print(f"Status: {r.status_code}")
    if r.status_code == 200:
        result = r.json()
        print("SUCCESS!")
        if "data" in result:
            for i, item in enumerate(result["data"]):
                if "b64_json" in item:
                    out = base64.b64decode(item["b64_json"])
                    outpath = r"d:\测试区域文件夹\app开发\test_output_gen.png"
                    with open(outpath, "wb") as f:
                        f.write(out)
                    print(f"Saved to: {outpath} ({len(out)} bytes)")
                elif "url" in item:
                    print(f"URL: {item['url'][:100]}")
    else:
        print(f"Error: {r.text[:300]}")
except Exception as e:
    print(f"Failed: {e}")

# Try chat completions with image (GPT-4o style)
print("\n--- Try 3: POST /v1/chat/completions with image ---")
try:
    img_b64 = base64.b64encode(image_bytes).decode()
    json_data = {
        "model": "gpt-4o",
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "image_url", "image_url": {"url": f"data:image/png;base64,{img_b64}"}},
                    {"type": "text", "text": PROMPT}
                ]
            }
        ],
        "max_tokens": 1000
    }
    r = requests.post(f"{BASE_URL}/v1/chat/completions", headers=headers, json=json_data, timeout=60)
    print(f"Status: {r.status_code}")
    if r.status_code == 200:
        result = r.json()
        print(f"Response: {json.dumps(result, ensure_ascii=False)[:500]}")
    else:
        print(f"Error: {r.text[:300]}")
except Exception as e:
    print(f"Failed: {e}")

print("\n" + "=" * 60)
print("Test complete.")
