import os
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from contextlib import asynccontextmanager # 최신 규격용 추가

from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

# --- 🛠️ 유틸리티 및 파이프라인 로직 ---
STATUS_FILE = "crawl_status.json"
PIPELINE_SCRIPTS = ["crawling.py", "cnn_pic_dec.py", "google_language_detector.py", "top10_filter.py"]

def get_today_str():
    return datetime.now().strftime("%Y-%m-%d")

def update_status(status_str):
    data = {}
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, "r", encoding="utf-8") as f:
            try: data = json.load(f)
            except: data = {}
    data[get_today_str()] = status_str
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
    """사용자가 지정한 4단계 순서대로 실행"""
    print(f"⏰ [{datetime.now()}] 파이프라인 가동 시작...")
    for script in PIPELINE_SCRIPTS:
        try:
            print(f"   ▶️ 실행 중: {script}")
            # 가상환경의 python으로 실행하여 라이브러리 충돌 방지
            subprocess.run([sys.executable, script], check=True)
        except Exception as e:
            print(f"   ❌ {script} 단계에서 오류 발생: {e}")
            update_status("failed")
            return
    update_status("success")
    print(f"✅ [{get_today_str()}] 모든 작업이 완료되었습니다.")

# --- 🚀 [핵심] Lifespan 이벤트 핸들러 (Warning 해결) ---
@asynccontextmanager
async def lifespan(app: FastAPI):
    # [Startup] 서버가 켜질 때 실행
    print("🚀 서버가 가동되었습니다. 데이터 업데이트 상태를 확인합니다.")
    if not check_success():
        print("❗️ 오늘자 성공 기록이 없습니다. 즉시 크롤링을 수행합니다.")
        run_full_pipeline()
    
    # 스케줄러 설정
    scheduler = BackgroundScheduler(timezone="Asia/Seoul")
    # 1. 매일 00:00 정기 실행
    scheduler.add_job(run_full_pipeline, CronTrigger(hour=0, minute=0))
    # 2. 매일 12:00 누락 확인 및 재시도
    scheduler.add_job(lambda: run_full_pipeline() if not check_success() else None, 
                      CronTrigger(hour=12, minute=0))
    scheduler.start()
    
    yield # 서버가 돌아가는 동안 대기
    
    # [Shutdown] 서버가 꺼질 때 실행
    scheduler.shutdown()

# --- 🌐 FastAPI 앱 선언 ---
app = FastAPI(lifespan=lifespan)

# CORS 설정 (안드로이드 접속용)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.mount("/static", StaticFiles(directory="."), name="static")

@app.get("/api/status")
async def get_crawl_status():
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"message": "기록이 없습니다."}

@app.get("/api/top10/{date}/{category}")
async def get_memes(date: str, category: str):
    file_path = Path(f"{date}/{category}/top10/{category}_top10.json")
    if file_path.exists():
        with open(file_path, "r", encoding="utf-8") as f:
            return json.load(f)
    return {"error": "해당 데이터를 찾을 수 없습니다."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)