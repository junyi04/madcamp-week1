import asyncio
import json
import random
import os
import requests
import shutil
import urllib.parse
from datetime import datetime
from pathlib import Path
from playwright.async_api import async_playwright

# --- 🚀 1. 설정 및 환경 변수 ---
STATUS_FILE = "crawl_status.json"
TODAY = datetime.now().strftime("%Y-%m-%d")
USER_DATA_DIR = f"C:/Users/{os.getlogin()}/AppData/Local/Google/Chrome/no_mor09"

def get_safe_url(query):
    # 한글 깨짐 방지 및 보안 파라미터(t=) 제거
    encoded_query = urllib.parse.quote(query)
    return f"https://www.tiktok.com/search?q={encoded_query}"

CATEGORIES = [
    {"name": "main", "urls": ["https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%80-%EC%88%9C%EC%9C%BC%EB%A1%9C-%EC%A0%95%EB%A0%AC", "https://www.tiktok.com/explore"]},
    {"name": "dance", "urls": ["https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%EC%B6%A4-2025", get_safe_url("틱톡 춤")]},
    {"name": "challenge", "urls": ["https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%ED%8B%B1%ED%86%A1-%EC%B1%8C%EB%A6%B0%EC%A7%80", get_safe_url("인기 챌린지")]},
    {"name": "food", "urls": ["https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EB%A8%B9%EB%B0%A9%EC%A1%B0%ED%9A%8C%EC%88%98-1%EC%9C%84-%EC%98%81%EC%83%81", get_safe_url("음식 레시피")]},
    {"name": "tts", "urls": [get_safe_url("tts 조회수"), get_safe_url("tts 밈")]}
]

# 📍 [복구] 서버에 실시간 상태를 보고하는 함수
def report_status(category, refresh_idx, count, stage="crawling"):
    status_data = {}
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, "r", encoding="utf-8") as f:
            try: status_data = json.load(f)
            except: status_data = {}
    
    status_data[TODAY] = {
        "status": "processing",
        "current_stage": stage,
        "current_category": category,
        "current_refresh": f"{refresh_idx + 1}/5",
        "current_count": count,
        "last_update": datetime.now().strftime("%H:%M:%S")
    }
    with open(STATUS_FILE, "w", encoding="utf-8") as f:
        json.dump(status_data, f, indent=2, ensure_ascii=False)

def initialize_folder():
    if os.path.exists(TODAY):
        try:
            shutil.rmtree(TODAY)
            print(f"🧹 오늘자({TODAY}) 데이터 폴더 초기화 완료")
        except PermissionError:
            print(f"⚠️ 서버 점유 중: 기존 폴더 유지")
    os.makedirs(TODAY, exist_ok=True)

def download_image_sync(url, save_path):
    try:
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        response = requests.get(url, timeout=5)
        if response.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(response.content)
            return True
    except: return False
    return False

async def run_intercept():
    async with async_playwright() as p:
        try:
            context = await p.chromium.launch_persistent_context(
                user_data_dir=USER_DATA_DIR,
                channel="chrome",
                headless=False,
                args=["--disable-blink-features=AutomationControlled", "--start-maximized"],
                viewport=None,
                slow_mo=random.randint(50, 100)
            )
            # 📍 [복구] 쿠키 삭제 기능
            await context.clear_cookies()
            print("🍪 쿠키 삭제 완료. 깨끗한 세션으로 시작합니다.")
        except Exception as e:
            print(f"❌ 브라우저 충돌: {e}")
            return

        page = await context.new_page()
        # 실시간 수집 상태 관리 객체 (이전 로직 완벽 복구)
        state = {"cat": "", "ids": set(), "list": [], "refresh": 0, "thumb_path": ""}

        async def handle_response(response):
            # 📍 [복구] 'search' 포함 모든 수집 패킷 감지
            if any(k in response.url for k in ["video_list", "discover", "item_list", "search"]):
                if response.status == 200:
                    try:
                        raw_data = await response.json()
                        videos = raw_data.get('videoList') or raw_data.get('itemList') or raw_data.get('data', [])
                        if not isinstance(videos, list): return

                        for raw_item in videos:
                            # 📍 [복구] 검색 결과 특유의 'item' 중첩 구조 해결
                            item = raw_item.get('item') if 'item' in raw_item else raw_item
                            v_id = item.get('id')
                            stats = item.get('stats', {})
                            play_count = stats.get('playCount', 0)

                            # 📍 [복구] 10만 조회수 필터 및 중복 제거
                            if v_id and v_id not in state["ids"] and play_count >= 100000:
                                author = item.get('author', {})
                                video_obj = item.get('video', {})
                                img_url = video_obj.get('cover') or video_obj.get('originCover')
                                
                                if not img_url: continue

                                # 📍 [복구] 상세 메타데이터 추출 (제목 보정 포함)
                                raw_desc = item.get('desc', '').strip()
                                final_title = raw_desc if raw_desc else "제목 없음"
                                img_filename = f"{v_id}.jpg"
                                img_save_path = os.path.join(state["thumb_path"], img_filename)

                                success = await asyncio.to_thread(download_image_sync, img_url, img_save_path)
                                if success:
                                    info = {
                                        "id": v_id,
                                        "title": final_title,
                                        "author": author.get('nickname', '알 수 없음'),
                                        "views": play_count,
                                        "likes": stats.get('diggCount', 0),
                                        "category": state["cat"],
                                        "url": f"https://www.tiktok.com/@{author.get('uniqueId')}/video/{v_id}",
                                        "image_file": f"{TODAY}/{state['cat']}/thumbnails/{img_filename}"
                                    }
                                    state["list"].append(info)
                                    state["ids"].add(v_id)
                                    report_status(state["cat"], state["refresh"], len(state["list"]))
                                    print(f"   [포착] {state['cat']} | {v_id} | {play_count:,}")
                    except: pass

        page.on("response", handle_response)

        # --- 🔄 [복구] 틱톡 카테고리/URL/새로고침 3중 루프 ---
        for cat in CATEGORIES:
            base_dir = Path(TODAY) / cat["name"]
            thumb_dir = base_dir / "thumbnails"
            thumb_dir.mkdir(parents=True, exist_ok=True)

            state.update({
                "cat": cat["name"],
                "thumb_path": str(thumb_dir),
                "ids": set(),
                "list": [],
                "refresh": 0
            })

            print(f"\n📂 카테고리 시작: {cat['name'].upper()}")
            for url_idx, url in enumerate(cat["urls"], 1):
                print(f"🔗 소스 {url_idx} 수집 중...")
                for r in range(5): # 📍 [복구] 새로고침 5회 로직
                    state["refresh"] = r
                    report_status(cat["name"], r, len(state["list"]))
                    print(f"   🔄 로딩 중... ({r+1}/5)")
                    
                    try:
                        await page.goto(url, wait_until="domcontentloaded", timeout=60000)
                        await asyncio.sleep(5)
                        for _ in range(15): # 📍 [복구] 심층 스크롤 15회 로직
                            await page.mouse.wheel(0, random.randint(4000, 6000))
                            await asyncio.sleep(random.uniform(2, 4))
                    except Exception as e:
                        print(f"   ⚠️ 타임아웃 발생(무시): {e}")

            # 💾 카테고리 최종 저장
            with open(base_dir / f"{cat['name']}_data.json", "w", encoding="utf-8") as f:
                json.dump(state["list"], f, ensure_ascii=False, indent=2)
            print(f"💾 {cat['name']} 통합 완료 ({len(state['list'])}건)")

        print(f"\n✨ [{TODAY}] 모든 카테고리 크롤링 완수!")
        await context.close()

if __name__ == "__main__":
    initialize_folder()
    asyncio.run(run_intercept())