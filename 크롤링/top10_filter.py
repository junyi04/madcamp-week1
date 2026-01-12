import os
import json
import shutil
from pathlib import Path
from datetime import datetime

# ===========================
# 🎛️ 설정 영역
# ===========================
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
TOP_LIMIT = 10
ARCHIVE_ROOT = Path("residual_archive")
RETENTION_DAYS = 3
STATUS_FILE = "crawl_status.json"
LOG_FILE = "execution_log.txt"
# ===========================

def update_status(status="success"):
    """crawl_status.json 업데이트"""
    data = {}
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, "r", encoding="utf-8") as f:
            try: data = json.load(f)
            except: data = {}
    data[TARGET_DATE_FOLDER] = status
    with open(STATUS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)

def write_execution_log(message):
    """execution_log.txt에 실행 시간 기록"""
    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    with open(LOG_FILE, "a", encoding="utf-8") as log:
        log.write(f"[{now}] {message}\n")

def safe_move(src, dst):
    """폴더/파일 이동 시 이미 존재하면 삭제 후 이동 (shutil.Error 방지)"""
    src, dst = Path(src), Path(dst)
    if not src.exists(): return
    if dst.exists():
        if dst.is_dir(): shutil.rmtree(dst)
        else: os.remove(dst)
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(src), str(dst))

def clean_old_archive():
    if not ARCHIVE_ROOT.exists(): return
    today = datetime.now()
    for folder in ARCHIVE_ROOT.iterdir():
        if folder.is_dir():
            try:
                folder_date = datetime.strptime(folder.name, "%Y-%m-%d")
                if (today - folder_date).days >= RETENTION_DAYS:
                    shutil.rmtree(folder)
            except: continue

def process_top10_with_main_merge():
    base_path = Path(TARGET_DATE_FOLDER)
    if not base_path.exists():
        print(f"❌ 폴더 없음: {TARGET_DATE_FOLDER}")
        return

    # --- Step 1: Main 폴더로 모든 데이터 물리적 복사 및 병합 ---
    main_dir = base_path / "main"
    main_thumb_dir = main_dir / "thumbnails"
    main_thumb_dir.mkdir(parents=True, exist_ok=True)
    
    main_total_list = []
    other_categories = [d for d in base_path.iterdir() if d.is_dir() and d.name != "main"]

    print(f"📦 [Step 1] 모든 데이터를 Main으로 통합 중...")
    for cat_dir in other_categories:
        json_path = cat_dir / f"{cat_dir.name}_data.json"
        if not json_path.exists(): continue

        with open(json_path, "r", encoding="utf-8") as f:
            try:
                cat_data = json.load(f)
                for item in cat_data:
                    # 썸네일 경로 확인 및 복사
                    old_img = Path(item['image_file'])
                    if not old_img.exists():
                        old_img = cat_dir / "thumbnails" / old_img.name
                    
                    new_img_path = main_thumb_dir / old_img.name
                    if old_img.exists() and not new_img_path.exists():
                        shutil.copy(old_img, new_img_path)
                    
                    # 제목 없음 보정 (Hole 반영)
                    if not item.get('title') or str(item['title']).strip() == "":
                        item['title'] = "제목 없음"
                    
                    item['image_file'] = str(new_img_path).replace("\\", "/")
                    main_total_list.append(item)
            except: continue

    # main_data.json 저장 (전체 데이터 합본)
    main_json_path = main_dir / "main_data.json"
    with open(main_json_path, "w", encoding="utf-8") as f:
        json.dump(main_total_list, f, ensure_ascii=False, indent=2)

    # --- Step 2: 모든 폴더(Main 포함)에서 TOP 10 추출 작업 수행 ---
    print(f"🚀 [Step 2] 카테고리별 TOP 10 추출 시작...")
    all_target_dirs = [d for d in base_path.iterdir() if d.is_dir()]
    
    for target_dir in all_target_dirs:
        cat_name = target_dir.name
        raw_json = target_dir / f"{cat_name}_data.json"
        if not raw_json.exists(): continue
        run_ranking_logic(target_dir, raw_json, cat_name)

    # --- Step 3: 정화, 보관 및 로그 기록 ---
    finalize_and_archive(base_path)
    write_execution_log("통합 메인 포함 모든 카테고리 정제 및 아카이브 완료")
    print(f"\n✨ 작업 완료! execution_log.txt 및 crawl_status.json 업데이트됨.")

def run_ranking_logic(target_dir, json_path, cat_name):
    with open(json_path, "r", encoding="utf-8") as f:
        data_list = json.load(f)
    
    # URL 중복 제거
    unique_map = {}
    for item in data_list:
        url = item.get("url")
        if url:
            if url not in unique_map or item.get('views', 0) > unique_map[url].get('views', 0):
                unique_map[url] = item

    # TOP 10 정렬
    top10_list = sorted(unique_map.values(), key=lambda x: x.get('views', 0), reverse=True)[:TOP_LIMIT]

    top10_dir = target_dir / "top10"
    top10_thumb_dir = top10_dir / "thumbnails"
    top10_thumb_dir.mkdir(parents=True, exist_ok=True)

    final_results = []
    for i, video in enumerate(top10_list, start=1):
        new_id = f"{cat_name}{str(i).zfill(2)}"
        old_img = Path(video['image_file'])
        new_img = top10_thumb_dir / f"{new_id}.jpg"
        
        if old_img.exists():
            shutil.copy(old_img, new_img)
        
        # 제목 없음 보정 (중첩 방어)
        if not video.get('title') or str(video['title']).strip() == "":
            video['title'] = "제목 없음"
            
        video['id'] = new_id
        video['image_file'] = f"/static/{TARGET_DATE_FOLDER}/{cat_name}/top10/thumbnails/{new_id}.jpg"
        final_results.append(video)

    with open(top10_dir / f"{cat_name}_top10.json", "w", encoding="utf-8") as f:
        json.dump(final_results, f, ensure_ascii=False, indent=2)
    print(f"   ✅ {cat_name} 완료")

def finalize_and_archive(base_path):
    archive_date_path = ARCHIVE_ROOT / TARGET_DATE_FOLDER
    archive_date_path.mkdir(parents=True, exist_ok=True)
    
    for cat_dir in [d for d in base_path.iterdir() if d.is_dir() and d.name != "top10"]:
        archive_cat = archive_date_path / cat_dir.name
        
        # 원본 thumbnails 이동 (safe_move 적용)
        src_thumb = cat_dir / "thumbnails"
        dst_thumb = archive_cat / "thumbnails"
        if src_thumb.exists():
            safe_move(src_thumb, dst_thumb)
            
        # 원본 raw json 이동 (safe_move 적용)
        raw_json = cat_dir / f"{cat_dir.name}_data.json"
        dst_json = archive_cat / f"{cat_dir.name}_raw.json"
        if raw_json.exists():
            safe_move(raw_json, dst_json)

    clean_old_archive()
    update_status("success")

if __name__ == "__main__":
    process_top10_with_main_merge()