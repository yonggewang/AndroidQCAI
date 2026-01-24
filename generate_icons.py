import os
from PIL import Image, ImageOps

source_image_path = "/Volumes/HDD/iOS/AndroidCLTDIY/AndroidApp/app/src/main/source_icon.png"
res_path = "/Volumes/HDD/iOS/AndroidCLTDIY/AndroidApp/app/src/main/res"

densities = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

def create_round_mask(size):
    mask = Image.new('L', size, 0)
    from PIL import ImageDraw
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0) + size, fill=255)
    return mask

def generate_icons():
    if not os.path.exists(source_image_path):
        print(f"Source image not found at {source_image_path}")
        return

    try:
        img = Image.open(source_image_path).convert("RGBA")
    except Exception as e:
        print(f"Failed to open source image: {e}")
        return

    for density, size in densities.items():
        folder_name = f"mipmap-{density}"
        folder_path = os.path.join(res_path, folder_name)
        os.makedirs(folder_path, exist_ok=True)

        # Standard Icon (Square/Full Bleed)
        # Note: Android adaptive icons are usually 108dp * density_scale, but for legacy/simple structure
        # we often use 48dp (mdpi) as base.
        # Actually standard sizes for legacy launcher icons are 48, 72, 96, 144, 192.
        
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        standard_out = os.path.join(folder_path, "ic_launcher.png")
        resized_img.save(standard_out)
        print(f"Generated {standard_out}")

        # Round Icon
        mask = create_round_mask((size, size))
        round_img = ImageOps.fit(img, (size, size), centering=(0.5, 0.5))
        round_img.putalpha(mask)
        
        round_out = os.path.join(folder_path, "ic_launcher_round.png")
        round_img.save(round_out)
        print(f"Generated {round_out}")

if __name__ == "__main__":
    generate_icons()
