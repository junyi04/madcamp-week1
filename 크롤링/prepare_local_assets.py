import os
import json
import shutil
from pathlib import Path
from datetime import datetime

# --- Configuration ---
# Assuming the script is in '크롤링' and the project root is its parent.
project_root = Path(__file__).parent.parent
crawl_root = project_root / "크롤링"
android_assets_root = project_root / "app" / "src" / "main" / "assets"
thumbnails_dest_dir = android_assets_root / "thumbnails"
final_json_path = android_assets_root / "video_data.json"

def find_latest_crawl_dir():
    """Finds the most recent directory named like 'YYYY-MM-DD' in the crawl_root."""
    date_dirs = [d for d in crawl_root.iterdir() if d.is_dir() and d.name.startswith('20')]
    if not date_dirs:
        return None
    return max(date_dirs)

def main():
    """
    Finds the latest crawled data, aggregates all top10 json files,
    copies their thumbnails to the android assets folder, and creates
    a final video_data.json with updated local asset paths for offline use.
    """
    print("🚀 안드로이드 로컬 assets 생성을 시작합니다...")

    latest_crawl_dir = find_latest_crawl_dir()
    if not latest_crawl_dir:
        print("❌ 크롤링된 데이터 폴더를 찾을 수 없습니다. (예: 2026-01-12)")
        return

    print(f"📂 대상 날짜 폴더: {latest_crawl_dir.name}")

    # --- 1. Ensure destination directories exist ---
    thumbnails_dest_dir.mkdir(exist_ok=True)
    print(f"✅ '{thumbnails_dest_dir}' 폴더를 준비했습니다.")

    # --- 2. Find all top10 json files ---
    top10_files = list(latest_crawl_dir.glob("**/top10/*_top10.json"))
    if not top10_files:
        print("❌ top10 JSON 파일들을 찾을 수 없습니다. 파이프라인을 먼저 실행하세요.")
        return

    print(f"📄 {len(top10_files)}개의 카테고리 JSON 파일을 찾았습니다.")

    master_data_list = []
    copied_images = set()

    # --- 3. Process each json file ---
    for json_path in top10_files:
        print(f"   - 처리 중: {json_path.name}")
        with open(json_path, "r", encoding="utf-8") as f:
            try:
                items = json.load(f)
            except json.JSONDecodeError:
                print(f"     ⚠️ JSON 파싱 오류, 건너뜁니다: {json_path.name}")
                continue
        
        for item in items:
            master_data_list.append(item)
            
            # --- 4. Copy image file ---
            image_path_str = item.get("image_file")
            if not image_path_str:
                continue

            image_name = Path(image_path_str).name
            
            # The image path in JSON is like '/static/2026-01-12/...', 
            # so we build the source path from the crawl_root.
            src_image_path = crawl_root / image_path_str.lstrip('/static/')

            if src_image_path.exists():
                if image_name not in copied_images:
                    dest_image_path = thumbnails_dest_dir / image_name
                    shutil.copy(src_image_path, dest_image_path)
                    copied_images.add(image_name)
            else:
                print(f"     ⚠️ 원본 이미지를 찾을 수 없습니다: {src_image_path}")


    print(f"🖼️ {len(copied_images)}개의 이미지를 assets 폴더로 복사했습니다.")

    # --- 5. Create final aggregated json with updated paths ---
    final_json_data = []
    for item in master_data_list:
        new_item = item.copy()
        image_name = Path(item.get("image_file", "")).name
        
        if image_name:
            # Rename key to match VideoData.kt and update path for local assets
            new_item["imageFile"] = f"file:///android_asset/thumbnails/{image_name}"
            new_item.pop("image_file", None)
        
        final_json_data.append(new_item)
    
    # --- 6. Write the final json file ---
    with open(final_json_path, "w", encoding="utf-8") as f:
        json.dump(final_json_data, f, ensure_ascii=False, indent=2)

    print(f"✅ 최종 데이터 파일 '{final_json_path}' 생성을 완료했습니다.")
    print(f"🎉 총 {len(final_json_data)}개의 항목이 포함되었습니다.")

if __name__ == "__main__":
    main()
