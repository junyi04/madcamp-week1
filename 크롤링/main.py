import os
import json
import subprocess
import sys
import threading
from datetime import datetime
from pathlib import Path
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

# ===========================
# 🎛️ 서버 및 환경 설정 (수정됨)
# ===========================
SERVER_IP = "10.249.86.17"
SERVER_PORT = 8001 # 8001로 통일
STATUS_FILE = "crawl_status.json"
PIPELINE_SCRIPTS = ["crawling.py", "cnn_pic_dec.py", "google_language_detector.py", "top10_filter.py"]

project_root = Path.cwd()
os.environ['TORCH_HOME'] = str(project_root / "models_cache")
# ===========================

def get_today_str():
    return datetime.now().strftime("%Y-%m-%d")

def check_success():
    if not os.path.exists(STATUS_FILE): return False
    with open(STATUS_FILE, "r", encoding="utf-8") as f:
        try:
            data = json.load(f)
            return data.get(get_today_str()) == "success"
        except: return False

def run_full_pipeline():
    print(f"⏰ [{datetime.now()}] 파이프라인 가동 시작...")
    for script in PIPELINE_SCRIPTS:
        try:
            print(f"   ▶️ 실행 중: {script}")
            subprocess.run([sys.executable, script], check=True)
        except Exception as e:
            print(f"   ❌ {script} 단계에서 오류 발생: {e}")
            return
    print(f"✅ 모든 작업이 완료되었습니다.")

@asynccontextmanager
async def lifespan(app: FastAPI):
    # 스케줄러 설정
    scheduler = BackgroundScheduler(timezone="Asia/Seoul")
    scheduler.add_job(run_full_pipeline, CronTrigger(hour=0, minute=0))
    scheduler.start()

    # 서버 시작 시 미수행 작업 확인 (daemon=True 추가)
    if not check_success():
        print("❗️ 오늘자 데이터가 없습니다. 백그라운드 실행을 시작합니다.")
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

# 정적 파일 마운트
app.mount("/static", StaticFiles(directory="."), name="static")

# 🌐 [핵심] 안드로이드용 통합 API
@app.get("/top10")
async def get_memes():
    today = get_today_str()
    # 경로가 top10_filter.py 결과물 위치와 맞아야 함
    file_path = Path(f"{today}/main/top10/main_top10.json")
    
    if file_path.exists():
        with open(file_path, "r", encoding="utf-8") as f:
            data = json.load(f)
            # 📍 로컬 경로를 웹 URL로 변환하는 핵심 로직
            for item in data:
                raw_path = item.get('image_file', '').replace("\\", "/").lstrip('/')
                item['imageFile'] = f"http://{SERVER_IP}:{SERVER_PORT}/{raw_path}"
                if not item.get('title'): item['title'] = "제목 없음"
            return data
    return {"error": "데이터를 아직 준비 중입니다."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=SERVER_PORT)