"""
快速测试 API 生成三视图效果。
用法: python test_api.py <输入图片路径>
例如: python test_api.py test_input.png
"""
import sys
import base64
import json
import requests

API_KEY = "sk-1o2GpxQKS1pqWE19kOJlwls7Ssd0t5jU35NByNthSJenxK3w"
BASE_URL = "https://api.bltcy.ai"

# Q版风格提示词
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

# 写实风格提示词
PROMPT_REALISTIC = """Scene:
Pure white background (#FFFFFF), no environment, no ground plane, no cast shadows.
Soft neutral studio lighting from upper-left to clearly define form and volume.

Subject:
Three orthographic projection views of the object from the input image, arranged HORIZONTALLY in a single row: Front View (left), Side View (center), Top View (right).
Transform the sketch/drawing into a REALISTIC 3D-model-ready reference with clear volume.

Important details:
- Each view occupies EXACTLY one-third of the total image width with EQUAL white gaps between them.
- REALISTIC proportions with clear volumetric form - show depth through subtle shading.
- Clean, sharp silhouette edges - this is critical for 3D AI reconstruction.
- Soft ambient occlusion to define where surfaces meet.
- Consistent neutral gray material appearance (like a clay/maquette render).
- NO heavy textures - keep surfaces smooth to emphasize form over detail.
- Each view at the SAME scale, perfectly aligned on the same baseline.
- Emphasize the overall 3D VOLUME and SILHOUETTE over surface details.
- Think of this as a sculptor's reference: form first, detail second.

Use case:
3D modeling reference sheet for AI-powered 3D reconstruction (Tripo, Meshy, etc).

Constraints:
- NO background elements, NO ground shadows, NO perspective distortion.
- NO excessive surface detail - prioritize clean silhouette and volume.
- NO labels, NO text, NO annotations, NO dimensions, NO watermark.
- Do NOT add busy textures or patterns that obscure the form.
- Do NOT deviate from the equal-thirds horizontal layout.
- Keep lighting CONSISTENT across all three views.
- Output image MUST be in 16:9 landscape aspect ratio (wider than tall)."""


def test_generate(image_path, style="chibi"):
    """调用API生成三视图"""
    prompt = PROMPT_CHIBI if style == "chibi" else PROMPT_REALISTIC
    
    # 读取图片
    with open(image_path, "rb") as f:
        image_bytes = f.read()
    
    # 尝试 OpenAI 兼容格式 (base64)
    image_b64 = base64.b64encode(image_bytes).decode()
    
    # 方式1: OpenAI Images Edit API 格式
    print(f"正在调用 API ({style} 风格)...")
    print(f"Base URL: {BASE_URL}")
    print(f"图片大小: {len(image_bytes)} bytes")
    print(f"提示词长度: {len(prompt)} chars")
    print("-" * 50)
    
    # 尝试 /v1/images/edits 端点 (OpenAI 标准)
    url = f"{BASE_URL}/v1/images/edits"
    headers = {
        "Authorization": f"Bearer {API_KEY}",
    }
    
    files = {
        "image": ("input.png", image_bytes, "image/png"),
    }
    data = {
        "prompt": prompt,
        "size": "1792x1024",
        "model": "gpt-image-1",
    }
    
    try:
        resp = requests.post(url, headers=headers, files=files, data=data, timeout=120)
        print(f"状态码: {resp.status_code}")
        
        if resp.status_code == 200:
            result = resp.json()
            print("成功！")
            # 保存结果
            if "data" in result and len(result["data"]) > 0:
                img_data = result["data"][0]
                if "b64_json" in img_data:
                    output_bytes = base64.b64decode(img_data["b64_json"])
                    output_path = image_path.replace(".png", f"_output_{style}.png").replace(".jpg", f"_output_{style}.png")
                    with open(output_path, "wb") as f:
                        f.write(output_bytes)
                    print(f"输出保存到: {output_path}")
                elif "url" in img_data:
                    print(f"输出URL: {img_data['url']}")
                    # 下载图片
                    img_resp = requests.get(img_data["url"])
                    output_path = image_path.replace(".png", f"_output_{style}.png").replace(".jpg", f"_output_{style}.png")
                    with open(output_path, "wb") as f:
                        f.write(img_resp.content)
                    print(f"输出保存到: {output_path}")
            else:
                print(f"响应内容: {json.dumps(result, indent=2, ensure_ascii=False)[:500]}")
        else:
            print(f"错误: {resp.text[:500]}")
            
            # 如果 edits 不行，尝试 /v1/images/generations
            print("\n尝试 /v1/images/generations 端点...")
            url2 = f"{BASE_URL}/v1/images/generations"
            json_data = {
                "model": "gpt-image-1",
                "prompt": f"Based on the input image (a cute character with red curly hair and blue top): {prompt}",
                "size": "1792x1024",
                "quality": "high",
                "n": 1,
            }
            resp2 = requests.post(url2, headers=headers, json=json_data, timeout=120)
            print(f"状态码: {resp2.status_code}")
            if resp2.status_code == 200:
                result2 = resp2.json()
                print(f"响应: {json.dumps(result2, indent=2, ensure_ascii=False)[:500]}")
            else:
                print(f"错误: {resp2.text[:500]}")
                
    except Exception as e:
        print(f"请求失败: {e}")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("用法: python test_api.py <图片路径> [chibi|realistic]")
        print("例如: python test_api.py input.png chibi")
        sys.exit(1)
    
    image_path = sys.argv[1]
    style = sys.argv[2] if len(sys.argv) > 2 else "chibi"
    test_generate(image_path, style)
