import os
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from apscheduler.schedulers.background import BackgroundScheduler
from apscheduler.triggers.cron import CronTrigger

app = FastAPI()

# --- ⚙️ 설정 및 경로 ---
STATUS_FILE = "crawl_status.json"
PIPELINE_SCRIPTS = ["crawling.py", "cnn_pic_dec.py", "google_language_detector.py", "top10_filter.py"]

# --- 🛠️ 유틸리티 함수 ---

def get_today_str():
    return datetime.now().strftime("%Y-%m-%d")

def update_status(status_str):
    """크롤링 성공 여부를 JSON에 저장"""
    data = {}
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, "r", encoding="utf-8") as f:
            try: data = json.load(f)
            except: data = {}
    
    data[get_today_str()] = status_str
    with open(STATUS_FILE, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

def check_success():
    """오늘 크롤링이 이미 성공했는지 확인"""
    if not os.path.exists(STATUS_FILE): return False
    with open(STATUS_FILE, "r", encoding="utf-8") as f:
        try:
            data = json.load(f)
            return data.get(get_today_str()) == "success"
        except: return False

def run_full_pipeline():
    """4단계 파이프라인 순차 실행"""
    today = get_today_str()
    print(f"⏰ [{datetime.now()}] 파이프라인 가동 시작...")
    
    for script in PIPELINE_SCRIPTS:
        try:
            # sys.executable을 사용하여 가상환경의 python으로 실행
            print(f"   ▶️ 실행 중: {script}")
            subprocess.run([sys.executable, script], check=True)
        except subprocess.CalledProcessError as e:
            print(f"   ❌ {script} 실행 실패. 중단합니다.")
            update_status("failed")
            return
    
    print(f"✅ [{today}] 모든 작업 성공!")
    update_status("success")

# --- 🗓️ 스케줄러 설정 ---

def scheduled_task():
    """00:00 정기 실행"""
    print("📢 00:00 정기 크롤링을 시작합니다.")
    run_full_pipeline()

def retry_check_task():
    """12:00 미이행 확인 및 재시도"""
    print("🔍 12:00 누락 여부 점검 중...")
    if not check_success():
        print("⚠️ 오늘 크롤링 기록이 없습니다. 재시도를 시작합니다.")
        run_full_pipeline()
    else:
        print("이미 오늘 크롤링이 완료되었습니다.")

scheduler = BackgroundScheduler(timezone="Asia/Seoul")

# 1. 매일 00:00 정기 실행
scheduler.add_job(scheduled_task, CronTrigger(hour=0, minute=0))

# 2. 매일 12:00 누락 확인 및 재시도
scheduler.add_job(retry_check_task, CronTrigger(hour=12, minute=0))

scheduler.start()

# --- 🚀 서버 시작 시 즉시 체크 (Hole 1 해결) ---
@app.on_event("startup")
async def startup_event():
    print("🚀 서버가 가동되었습니다. 오늘자 데이터 상태를 확인합니다.")
    if not check_success():
        print("❗️ 오늘자 성공 기록이 없습니다. 즉시 크롤링을 수행합니다.")
        # 서버 시작하자마자 실행 (백그라운드에서 실행하려면 threading 등을 고려할 수 있으나 여기서는 직접 호출)
        run_full_pipeline()

# --- 🌐 FastAPI 엔드포인트 ---

app.mount("/static", StaticFiles(directory="."), name="static")

@app.get("/api/status")
async def get_status():
    """크롤링 기록 확인용 API"""
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
    return {"error": "데이터가 없습니다."}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)