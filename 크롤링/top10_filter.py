import mysql.connector
from mysql.connector import Error
import os
import json
import shutil
from pathlib import Path
from datetime import datetime, timedelta

# ===========================
# 🎛️ 설정 영역
# ===========================
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
TOP_LIMIT = 10
ARCHIVE_ROOT = Path("residual_archive")
RETENTION_DAYS = 3
STATUS_FILE = "crawl_status.json"
LOG_FILE = "execution_log.txt"

# MySQL 설정
MYSQL_CONFIG = {
    'host': 'localhost',
    'database': 'madcamp1_db',
    'user': 'root',
    'password': '4038'
}

SERVER_DOMAIN = "young-forty.ngrok.app"

# ===========================
# 🗄️ MySQL 날짜 기반 관리 함수
# ===========================
def clear_old_data():
    """3일 이상 된 데이터 자동 삭제 (파일 시스템과 동일)"""
    print("🗑️ 오래된 DB 데이터 정리 중...")
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor()
        
        # 3일 전 날짜 계산
        three_days_ago = (datetime.now() - timedelta(days=RETENTION_DAYS)).strftime("%Y-%m-%d")
        
        # Foreign Key 체크 비활성화
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        
        # 3일 이상 된 데이터만 삭제
        cursor.execute("DELETE FROM tiktok_videos WHERE DATE(created_at) < %s", (three_days_ago,))
        deleted_main = cursor.rowcount
        
        cursor.execute("DELETE FROM candidate_videos WHERE DATE(created_at) < %s", (three_days_ago,))
        deleted_cand = cursor.rowcount
        
        cursor.execute("DELETE FROM filtered_non_korean WHERE DATE(created_at) < %s", (three_days_ago,))
        deleted_nk = cursor.rowcount
        
        cursor.execute("DELETE FROM filtered_duplicates WHERE DATE(created_at) < %s", (three_days_ago,))
        deleted_dup = cursor.rowcount
        
        # Foreign Key 체크 다시 활성화
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
        
        connection.commit()
        
        if deleted_main > 0 or deleted_cand > 0:
            print(f"✅ 오래된 데이터 정리 완료:")
            print(f"   - Top10: {deleted_main}건")
            print(f"   - 후보군: {deleted_cand}건")
            print(f"   - 비한국어: {deleted_nk}건")
            print(f"   - 중복: {deleted_dup}건")
        else:
            print("   (삭제할 오래된 데이터 없음)")
        
    except Error as e:
        print(f"⚠️ 데이터 정리 실패: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

def clear_today_data():
    """오늘 날짜 데이터만 초기화 (재실행 대비)"""
    print("🗑️ 오늘 데이터 초기화 중...")
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor()
        
        today = datetime.now().strftime("%Y-%m-%d")
        
        # Foreign Key 체크 비활성화
        cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        
        # 오늘 날짜 데이터만 삭제
        cursor.execute("DELETE FROM candidate_videos WHERE filtered_date = %s", (today,))
        cursor.execute("DELETE FROM tiktok_videos WHERE DATE(created_at) = %s", (today,))
        cursor.execute("DELETE FROM filtered_non_korean WHERE filtered_date = %s", (today,))
        cursor.execute("DELETE FROM filtered_duplicates WHERE filtered_date = %s", (today,))
        
        # Foreign Key 체크 다시 활성화
        cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
        
        connection.commit()
        print("✅ 오늘 데이터 초기화 완료")
        
    except Error as e:
        print(f"⚠️ 초기화 실패: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

# ===========================
# 🗄️ MySQL 저장 함수
# ===========================
def save_to_mysql(top10_data, category):
    """Top10 데이터를 MySQL에 저장"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        
        if connection.is_connected():
            cursor = connection.cursor()
            
            # 해당 카테고리 오늘 날짜 데이터만 삭제
            today = datetime.now().strftime("%Y-%m-%d")
            cursor.execute("""DELETE FROM tiktok_videos 
                            WHERE category = %s AND DATE(created_at) = %s""", 
                          (category, today))
            print(f"   🗑️ 기존 {category} 데이터 삭제 완료")
            
            # 새 데이터 삽입
            sql = """INSERT INTO tiktok_videos 
                     (id, title, author, views, likes, category, url, image_url) 
                     VALUES (%s, %s, %s, %s, %s, %s, %s, %s)"""
            
            for video in top10_data:
                # 이미지 경로를 HTTPS URL로 변환
                local_path = video.get('image_file', '').replace('\\', '/').lstrip('/')
                image_https_url = f"https://{SERVER_DOMAIN}/{local_path}"
                
                cursor.execute(sql, (
                    video.get('id'),
                    video.get('title', '제목 없음'),
                    video.get('author', '알 수 없음'),
                    video.get('views', 0),
                    video.get('likes', 0),
                    category,
                    video.get('url', ''),
                    image_https_url
                ))
            
            connection.commit()
            print(f"   💾 MySQL 저장 완료: {category} ({len(top10_data)}건)")
            
    except Error as e:
        print(f"   ❌ MySQL 에러: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

def save_candidates_to_mysql(all_data, top10_ids, category, filtered_date):
    """Top10 못 들어간 후보군을 MySQL에 저장"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor()
        
        # Top10에 속하지 않은 데이터만 필터
        candidates = [item for item in all_data if item.get('id') not in top10_ids]
        candidates = sorted(candidates, key=lambda x: x.get('views', 0), reverse=True)
        
        # 해당 카테고리 오늘 날짜 데이터만 삭제
        cursor.execute("""DELETE FROM candidate_videos 
                         WHERE category = %s AND filtered_date = %s""", 
                      (category, filtered_date))
        
        # 중복 방지: ON DUPLICATE KEY UPDATE
        sql = """INSERT INTO candidate_videos 
                 (id, title, author, views, likes, category, url, image_url, 
                  rank_in_category, filtered_date) 
                 VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                 ON DUPLICATE KEY UPDATE 
                 views=VALUES(views), 
                 rank_in_category=VALUES(rank_in_category)"""
        
        for rank, item in enumerate(candidates, start=11):
            # 이미지 URL 변환
            img_path = item.get('image_file', '')
            if img_path:
                local_path = img_path.replace('\\', '/').lstrip('/')
                image_url = f"https://{SERVER_DOMAIN}/{local_path}"
            else:
                image_url = None
            
            cursor.execute(sql, (
                f"cand_{category}_{item.get('id')}",
                item.get('title', '제목 없음'),
                item.get('author', '알 수 없음'),
                item.get('views', 0),
                item.get('likes', 0),
                category,
                item.get('url', ''),
                image_url,
                rank,
                filtered_date
            ))
        
        connection.commit()
        print(f"   💾 후보군 저장: {category} {len(candidates)}건")
        
    except Error as e:
        print(f"   ❌ MySQL 에러: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

# ===========================
# 📁 파일 시스템 함수
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
    """폴더/파일 이동 시 이미 존재하면 삭제 후 이동"""
    src, dst = Path(src), Path(dst)
    if not src.exists(): return
    if dst.exists():
        if dst.is_dir(): shutil.rmtree(dst)
        else: os.remove(dst)
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.move(str(src), str(dst))

def clean_old_archive():
    """오래된 아카이브 삭제 (3일 이상)"""
    if not ARCHIVE_ROOT.exists(): return
    today = datetime.now()
    for folder in ARCHIVE_ROOT.iterdir():
        if folder.is_dir():
            try:
                folder_date = datetime.strptime(folder.name, "%Y-%m-%d")
                if (today - folder_date).days >= RETENTION_DAYS:
                    shutil.rmtree(folder)
                    print(f"   🗑️ 오래된 아카이브 삭제: {folder.name}")
            except: continue

# ===========================
# 🎯 메인 로직
# ===========================
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
                    
                    # 제목 없음 보정
                    if not item.get('title') or str(item['title']).strip() == "":
                        item['title'] = "제목 없음"
                    
                    item['image_file'] = str(new_img_path).replace("\\", "/")
                    main_total_list.append(item)
            except: continue

    # main_data.json 저장
    main_json_path = main_dir / "main_data.json"
    with open(main_json_path, "w", encoding="utf-8") as f:
        json.dump(main_total_list, f, ensure_ascii=False, indent=2)

    # --- Step 2: 모든 폴더(Main 포함)에서 TOP 10 추출 ---
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
    """카테고리별 Top10 선정 로직"""
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
    top10_ids = []
    
    for i, video in enumerate(top10_list, start=1):
        new_id = f"{cat_name}{str(i).zfill(2)}"
        old_img = Path(video['image_file'])
        new_img = top10_thumb_dir / f"{new_id}.jpg"
        
        if old_img.exists():
            shutil.copy(old_img, new_img)
        
        # 제목 없음 보정
        if not video.get('title') or str(video['title']).strip() == "":
            video['title'] = "제목 없음"
            
        video['id'] = new_id
        video['image_file'] = f"{TARGET_DATE_FOLDER}/{cat_name}/top10/thumbnails/{new_id}.jpg"
        final_results.append(video)
        top10_ids.append(new_id)

    # JSON 파일 저장
    with open(top10_dir / f"{cat_name}_top10.json", "w", encoding="utf-8") as f:
        json.dump(final_results, f, ensure_ascii=False, indent=2)
    
    # MySQL 저장
    save_to_mysql(final_results, cat_name)
    save_candidates_to_mysql(list(unique_map.values()), top10_ids, cat_name, TARGET_DATE_FOLDER)
    
    print(f"   ✅ {cat_name} 완료")

def finalize_and_archive(base_path):
    """원본 데이터 아카이브 및 정리"""
    archive_date_path = ARCHIVE_ROOT / TARGET_DATE_FOLDER
    archive_date_path.mkdir(parents=True, exist_ok=True)
    
    for cat_dir in [d for d in base_path.iterdir() if d.is_dir() and d.name != "top10"]:
        archive_cat = archive_date_path / cat_dir.name
        
        # 원본 thumbnails 이동
        src_thumb = cat_dir / "thumbnails"
        dst_thumb = archive_cat / "thumbnails"
        if src_thumb.exists():
            safe_move(src_thumb, dst_thumb)
            
        # 원본 raw json 이동
        raw_json = cat_dir / f"{cat_dir.name}_data.json"
        dst_json = archive_cat / f"{cat_dir.name}_raw.json"
        if raw_json.exists():
            safe_move(raw_json, dst_json)

    clean_old_archive()
    update_status("success")

# ===========================
# 🚀 실행
# ===========================
if __name__ == "__main__":
    print(f"🚀 [{TARGET_DATE_FOLDER}] Top10 Ranking Pipeline 시작\n")
    
    # ⭐ 날짜 기반 데이터 관리 (파일 시스템과 동일)
    clear_old_data()      # 3일 이상 된 데이터 삭제
    clear_today_data()    # 오늘 데이터만 초기화 (재실행 대비)
    
    # 메인 로직 실행
    process_top10_with_main_merge()
