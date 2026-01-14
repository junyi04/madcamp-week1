import os
import json
import subprocess
import sys
import threading
from datetime import datetime
from pathlib import Path
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Query
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger
import mysql.connector
from mysql.connector import Error

# ===========================
# 🎛️ 서버 및 환경 설정
# ===========================
SERVER_DOMAIN = "young-forty.ngrok.app" 
SERVER_PORT = 8001 
STATUS_FILE = "crawl_status.json"
PIPELINE_SCRIPTS = ["crawling.py", "cnn_pic_dec.py", "google_language_detector.py", "top10_filter.py"]

# MySQL 설정
MYSQL_CONFIG = {
    'host': 'localhost',
    'database': 'madcamp1_db',
    'user': 'root',
    'password': '4038'
}

project_root = Path.cwd()
os.environ['TORCH_HOME'] = str(project_root / "models_cache")

def get_today_str():
    return datetime.now().strftime("%Y-%m-%d")

def mark_as_success():
    today = get_today_str()
    data = {}
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, "r", encoding="utf-8") as f:
            try: data = json.load(f)
            except: data = {}
    
    data[today] = "success"
    with open(STATUS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

def check_success():
    if not os.path.exists(STATUS_FILE): return False
    with open(STATUS_FILE, "r", encoding="utf-8") as f:
        try:
            data = json.load(f)
            return data.get(get_today_str()) == "success"
        except: return False

def run_full_pipeline():
    print(f"⏰ [{datetime.now()}] 전체 파이프라인 순차 실행 시작...")
    
    for script in PIPELINE_SCRIPTS:
        if not os.path.exists(script):
            print(f" ⚠️ 스킵: {script} 파일을 찾을 수 없습니다. 다음 단계로 넘어갑니다.")
            continue

        try:
            print(f"   ▶️ 현재 실행 중: {script}")
            subprocess.run([sys.executable, script], check=True)
            print(f"   ✅ {script} 완료")
        except Exception as e:
            print(f"   ❌ {script} 단계에서 오류 발생(무시하고 다음 단계 진행): {e}")
            continue 

    mark_as_success()
    print(f"✨ 모든 작업 순서가 완료되었습니다.")

@asynccontextmanager
async def lifespan(app: FastAPI):
    scheduler = BackgroundScheduler(timezone="Asia/Seoul")
    scheduler.add_job(run_full_pipeline, CronTrigger(hour=0, minute=0))
    scheduler.start()

    if not check_success():
        print("❗️ 오늘자 데이터 수집을 시작합니다.")
        thread = threading.Thread(target=run_full_pipeline, daemon=True)
        thread.start()

    yield
    scheduler.shutdown()

app = FastAPI(lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ===========================
# 🗄️ MySQL 조회 함수
# ===========================
def get_mysql_videos(category=None):
    """MySQL에서 비디오 데이터 조회"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor(dictionary=True)
        
        if category:
            sql = """SELECT id, title, author, views, likes, category, url, 
                     image_url as imageFile, created_at
                     FROM tiktok_videos 
                     WHERE category = %s 
                     ORDER BY views DESC"""
            cursor.execute(sql, (category,))
        else:
            sql = """SELECT id, title, author, views, likes, category, url, 
                     image_url as imageFile, created_at
                     FROM tiktok_videos 
                     ORDER BY category, views DESC"""
            cursor.execute(sql)
        
        results = cursor.fetchall()
        
        for item in results:
            if not item.get('title') or item['title'].strip() == "":
                item['title'] = "제목 없음"
        
        return results
        
    except Error as e:
        print(f"MySQL 에러: {e}")
        return None
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

# ===========================
# 📡 API 엔드포인트
# ===========================

@app.get("/")
async def root():
    """API 상태 확인"""
    return {
        "status": "online",
        "server": SERVER_DOMAIN,
        "message": "TikTok Meme API is running!",
        "endpoints": {
            "main": "/top10",
            "category": "/api/category/{name}",
            "all": "/api/all-videos",
            "categories": "/api/categories",
            "structure": "/api/file-structure",
            "latest": "/api/latest-date",
            "filtered": {
                "non_korean": "/api/filtered/non-korean",
                "duplicates": "/api/filtered/duplicates",
                "candidates": "/api/candidates"
            }
        }
    }

@app.get("/top10")
async def get_top10():
    """Main 카테고리 Top10 반환"""
    data = get_mysql_videos("main")
    
    if data and len(data) > 0:
        return data
    
    # 백업: JSON 파일
    today = get_today_str()
    file_path = Path(f"{today}/main/top10/main_top10.json")
    
    if file_path.exists():
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            for item in data:
                raw_path = item.get('image_file', '').replace("\\", "/").lstrip('/')
                item['imageFile'] = f"https://{SERVER_DOMAIN}/{raw_path}"
                if not item.get('title'): 
                    item['title'] = "제목 없음"
            return data
    
    return {"error": "데이터를 찾을 수 없습니다."}

@app.get("/api/category/{category_name}")
async def get_category(category_name: str):
    """특정 카테고리 Top10 반환"""
    data = get_mysql_videos(category_name)
    
    if data and len(data) > 0:
        return data
    
    return {"error": f"카테고리 '{category_name}' 데이터를 찾을 수 없습니다."}

@app.get("/api/all-videos")
async def get_all_videos():
    """모든 카테고리 통합 데이터 반환 (50개)"""
    data = get_mysql_videos()
    
    if data and len(data) > 0:
        return data
    
    return {"error": "데이터를 찾을 수 없습니다."}

@app.get("/api/categories")
async def get_categories():
    """카테고리별 그룹화된 데이터 반환"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor(dictionary=True)
        
        sql = """SELECT category, COUNT(*) as count 
                 FROM tiktok_videos 
                 GROUP BY category"""
        cursor.execute(sql)
        categories = cursor.fetchall()
        
        result = {}
        for cat in categories:
            cat_name = cat['category']
            cat_data = get_mysql_videos(cat_name)
            result[cat_name] = {
                "count": cat['count'],
                "videos": cat_data
            }
        
        return result
        
    except Error as e:
        return {"error": str(e)}
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

@app.get("/api/file-structure")
async def get_file_structure():
    """날짜 폴더의 전체 구조 반환 (Android가 폴더처럼 사용)"""
    today = get_today_str()
    base_path = Path(today)
    
    if not base_path.exists():
        return {"error": "데이터 폴더가 없습니다.", "date": today}
    
    structure = {
        "date": today,
        "base_url": f"https://{SERVER_DOMAIN}/{today}",
        "categories": {}
    }
    
    for cat_dir in base_path.iterdir():
        if not cat_dir.is_dir():
            continue
        
        cat_name = cat_dir.name
        top10_dir = cat_dir / "top10"
        
        if not top10_dir.exists():
            continue
        
        json_file = top10_dir / f"{cat_name}_top10.json"
        thumbnails_dir = top10_dir / "thumbnails"
        
        # JSON 읽기
        top10_data = []
        if json_file.exists():
            with open(json_file, "r", encoding="utf-8") as f:
                top10_data = json.load(f)
                # imageFile 변환
                for item in top10_data:
                    if 'image_file' in item:
                        item['imageFile'] = f"https://{SERVER_DOMAIN}/{item['image_file'].lstrip('/')}"
        
        # 이미지 목록
        images = []
        if thumbnails_dir.exists():
            images = sorted([f.name for f in thumbnails_dir.glob("*.jpg")])
        
        structure["categories"][cat_name] = {
            "json_url": f"https://{SERVER_DOMAIN}/{today}/{cat_name}/top10/{cat_name}_top10.json",
            "json_data": top10_data,
            "thumbnail_count": len(images),
            "thumbnails": [
                f"https://{SERVER_DOMAIN}/{today}/{cat_name}/top10/thumbnails/{img}"
                for img in images
            ]
        }
    
    return structure

@app.get("/api/latest-date")
async def get_latest_date():
    """가장 최근 날짜 폴더 찾기"""
    date_folders = [d for d in Path(".").iterdir() 
                   if d.is_dir() and d.name.startswith("20")]
    if not date_folders:
        return {"error": "날짜 폴더가 없습니다."}
    
    latest = max(date_folders, key=lambda x: x.name)
    return {"latest_date": latest.name}

@app.get("/api/filtered/non-korean")
async def get_non_korean(category: str = None):
    """한국어 아닌 것 필터링 데이터 조회"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor(dictionary=True)
        
        if category:
            sql = "SELECT * FROM filtered_non_korean WHERE category = %s ORDER BY views DESC"
            cursor.execute(sql, (category,))
        else:
            sql = "SELECT * FROM filtered_non_korean ORDER BY category, views DESC"
            cursor.execute(sql)
        
        results = cursor.fetchall()
        return results
        
    except Error as e:
        return {"error": str(e)}
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

@app.get("/api/filtered/duplicates")
async def get_duplicates():
    """중복 필터링 데이터 조회"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor(dictionary=True)
        
        sql = "SELECT * FROM filtered_duplicates ORDER BY similarity_score DESC"
        cursor.execute(sql)
        results = cursor.fetchall()
        return results
        
    except Error as e:
        return {"error": str(e)}
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

@app.get("/api/candidates")
async def get_candidates(category: str = None):
    """Top10 후보군 조회"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor(dictionary=True)
        
        if category:
            sql = """SELECT * FROM candidate_videos 
                    WHERE category = %s 
                    ORDER BY rank_in_category"""
            cursor.execute(sql, (category,))
        else:
            sql = "SELECT * FROM candidate_videos ORDER BY category, rank_in_category"
            cursor.execute(sql)
        
        results = cursor.fetchall()
        return results
        
    except Error as e:
        return {"error": str(e)}
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()

# ===========================
# 📁 Static 파일 서빙 (맨 마지막!)
# ===========================
app.mount("/", StaticFiles(directory=".", html=True), name="static")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=SERVER_PORT)