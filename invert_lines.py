"""
将 assets/ 中的黑线条图片转换为白线条版本。
逻辑：将深色像素（接近黑色）转为白色，保持透明通道不变。
输出到 assets_white/ 目录。
"""
from PIL import Image
import numpy as np
import os

input_dir = r"d:\测试区域文件夹\app开发\assets"
output_dir = r"d:\测试区域文件夹\app开发\assets_white"
os.makedirs(output_dir, exist_ok=True)

for filename in sorted(os.listdir(input_dir)):
    if not filename.lower().endswith(".png"):
        continue
    
    img = Image.open(os.path.join(input_dir, filename)).convert("RGBA")
    data = np.array(img)
    
    # Invert RGB channels (black becomes white, white becomes black)
    # But keep alpha channel unchanged
    rgb = data[:, :, :3]
    alpha = data[:, :, 3:]
    
    inverted_rgb = 255 - rgb
    result = np.concatenate([inverted_rgb, alpha], axis=2)
    
    out_img = Image.fromarray(result.astype(np.uint8), "RGBA")
    out_img.save(os.path.join(output_dir, filename))
    print(f"Converted: {filename}")

print(f"\nDone! {len(os.listdir(output_dir))} white-line images saved to {output_dir}")
