import os
import json
import shutil
from pathlib import Path
from datetime import datetime
from PIL import Image

# ===========================
# 🎛️ 사용자 파라미터 조절 영역
# ===========================
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") # "2026-01-09"
TOP_LIMIT = 10
ARCHIVE_ROOT = Path("residual_archive")
RETENTION_DAYS = 3
# ===========================

def clean_old_archive():
    """3일 지난 보관소 폴더를 삭제합니다."""
    if not ARCHIVE_ROOT.exists(): return
    today = datetime.now()
    for date_folder in ARCHIVE_ROOT.iterdir():
        if date_folder.is_dir():
            try:
                folder_date = datetime.strptime(date_folder.name, "%Y-%m-%d")
                if (today - folder_date).days >= RETENTION_DAYS:
                    shutil.rmtree(date_folder)
            except: continue

def process_top10_final_fix():
    base_path = Path(TARGET_DATE_FOLDER)
    if not base_path.exists():
        print(f"❌ 폴더 없음: {TARGET_DATE_FOLDER}"); return

    archive_date_path = ARCHIVE_ROOT / TARGET_DATE_FOLDER
    archive_date_path.mkdir(parents=True, exist_ok=True)

    # 1. 카테고리별 순회
    for cat_dir in [d for d in base_path.iterdir() if d.is_dir() and d.name != "top10"]:
        cat_name = cat_dir.name
        json_file = cat_dir / f"{cat_name}_data.json"
        if not json_file.exists(): continue

        # ⭐ [중복 봉쇄 핵심] URL을 키로 사용하여 데이터 중복 원천 차단
        unique_videos_map = {}
        with open(json_file, "r", encoding="utf-8") as f:
            for line in f:
                try:
                    data = json.loads(line)
                    v_url = data.get("url")
                    if not v_url: continue
                    
                    # 동일 URL이 있다면 조회수가 더 높은 레코드만 유지
                    if v_url not in unique_videos_map or data.get('views', 0) > unique_videos_map[v_url].get('views', 0):
                        unique_videos_map[v_url] = data
                except: continue

        # 2. 유일한 영상들만 조회수 순으로 정렬
        sorted_unique_videos = sorted(unique_videos_map.values(), key=lambda x: x.get('views', 0), reverse=True)
        top10_list = sorted_unique_videos[:TOP_LIMIT]

        # 3. TOP 10 저장 및 ID 리매핑
        top10_dir = cat_dir / "top10"
        top10_thumb_dir = top10_dir / "thumbnails"
        top10_thumb_dir.mkdir(parents=True, exist_ok=True)

        new_top10_data = []
        for i, video in enumerate(top10_list, start=1):
            new_id = f"{cat_name}{str(i).zfill(2)}"
            old_img = Path(video['image_file'])
            new_img = top10_thumb_dir / f"{new_id}.jpg"
            
            if old_img.exists():
                shutil.copy(old_img, new_img) # 사진 복사
            
            video['id'] = new_id
            video['image_file'] = str(new_img).replace("\\", "/")
            new_top10_data.append(video)

        with open(top10_dir / f"{cat_name}_top10.json", "w", encoding="utf-8") as f:
            for v in new_top10_data:
                f.write(json.dumps(v, ensure_ascii=False) + "\n")

        print(f"✅ [{cat_name}] 유니크 TOP {len(new_top10_data)} 추출 완료")

        # 4. ⭐ [정화 로직] 기존 thumbnails 폴더 및 원본 JSON 제거 (보관소로 이동)
        archive_cat_path = archive_date_path / cat_name
        archive_cat_path.mkdir(parents=True, exist_ok=True)
        
        # 원본 thumbnails 폴더 자체를 보관소로 이동
        original_thumbnails = cat_dir / "thumbnails"
        if original_thumbnails.exists():
            shutil.move(str(original_thumbnails), str(archive_cat_path / "thumbnails"))
        
        # 원본 JSON 파일 이동
        shutil.move(str(json_file), str(archive_cat_path / f"{cat_name}_raw.json"))

    # 5. 보관소 정리
    clean_old_archive()
    print(f"\n✨ 작업 완료! 기존 폴더의 thumbnails는 사라졌으며, 오직 top10 데이터만 남았습니다.")

if __name__ == "__main__":
    process_top10_final_fix()