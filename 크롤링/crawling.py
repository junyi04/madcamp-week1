import asyncio
import json
import random
import os
import requests
import shutil
from datetime import datetime
from pathlib import Path
from playwright.async_api import async_playwright

# --- 🚀 1. 설정 및 환경 변수 (URL 리스트 형태로 확장) ---
CATEGORIES = [
    {
        "name": "main", 
        "urls": [
            "https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%80-%EC%88%9C%EC%9C%BC%EB%A1%9C-%EC%A0%95%EB%A0%AC",
            "https://www.tiktok.com/explore" # 예시 추가 URL
        ]
    },
    {
        "name": "dance", 
        "urls": [
            "https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%EC%B6%A4-2025",
            "https://www.tiktok.com/search?q=%EC%B6%A4&t=1768204200785" # 예시 추가 URL
        ]
    },
    {
        "name": "challenge", 
        "urls": [
            "https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%ED%8B%B1%ED%86%A1-%EC%B1%8C%EB%A6%B0%EC%A7%80",
            "https://www.tiktok.com/search?q=%EC%B1%8C%EB%A6%B0%EC%A7%80&t=1768204248554" # 예시 추가 URL
        ]
    },
    {
        "name": "food", 
        "urls": [
            "https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EB%A8%B9%EB%B0%A9%EC%A1%B0%ED%9A%8C%EC%88%98-1%EC%9C%84-%EC%98%81%EC%83%81",
            "https://www.tiktok.com/search?q=%EC%9D%8C%EC%8B%9D&t=1768204267725" # 예시 추가 URL
        ]
    },
    {
        "name": "tts", 
        "urls": [
            "https://www.tiktok.com/search?q=tts%20%EC%A1%B0%ED%9A%8C%EC%88%98&t=1768204362144",
            "https://www.tiktok.com/search?q=tts&t=1768204284646" # 예시 추가 URL
        ]
    }
]

TODAY = datetime.now().strftime("%Y-%m-%d")
USER_DATA_DIR = f"C:/Users/{os.getlogin()}/AppData/Local/Google/Chrome/no_mor09"

def initialize_folder():
    if os.path.exists(TODAY):
        try:
            print(f"🧹 오늘자({TODAY}) 폴더를 초기화합니다.")
            shutil.rmtree(TODAY)
        except PermissionError:
            print(f"⚠️ 서버 점유 중: 기존 폴더를 유지하며 데이터를 업데이트합니다.")
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
                args=["--disable-blink-features=AutomationControlled"],
                slow_mo=50
            )
        except Exception as e:
            print(f"❌ 브라우저 충돌: {e}")
            return

        page = await context.new_page()
        
        # 실시간 상태 관리 (전역 수준이 아닌 카테고리 내부용)
        status = {"cat_name": "", "thumb_path": "", "seen_ids": set(), "current_list": []}

        async def handle_response(response):
            if any(k in response.url for k in ["video_list", "discover", "item_list"]):
                if response.status == 200:
                    try:
                        raw_data = await response.json()
                        videos = raw_data.get('videoList') or raw_data.get('itemList') or raw_data.get('data', [])
                        if not videos: return

                        for item in videos:
                            v_id = item.get('id')
                            play_count = item.get('stats', {}).get('playCount', 0)

                            # 📍 10만 조회수 필터링 + 중복 아이디 체크
                            if v_id and v_id not in status["seen_ids"] and play_count >= 100000:
                                author = item.get('author', {})
                                img_url = item.get('video', {}).get('cover') or item.get('video', {}).get('originCover')
                                if not img_url: continue

                                raw_desc = item.get('desc', '').strip()
                                final_title = raw_desc if raw_desc else "제목 없음"

                                img_path = os.path.join(status["thumb_path"], f"{v_id}.jpg")
                                success = await asyncio.to_thread(download_image_sync, img_url, img_path)

                                if success:
                                    info = {
                                        "id": v_id,
                                        "title": final_title,
                                        "author": author.get('nickname', '알 수 없음'),
                                        "views": play_count,
                                        "likes": item.get('stats', {}).get('diggCount', 0),
                                        "category": status["cat_name"],
                                        "url": f"https://www.tiktok.com/@{author.get('uniqueId')}/video/{v_id}",
                                        "image_file": img_path.replace("\\", "/")
                                    }
                                    status["current_list"].append(info)
                                    status["seen_ids"].add(v_id)
                                    print(f"   [포착] {status['cat_name']} | {v_id} | {play_count:,}")
                    except: pass

        page.on("response", handle_response)

        # --- 🔄 6. 카테고리별 순회 및 멀티 URL 처리 ---
        for cat in CATEGORIES:
            base_dir = Path(TODAY) / cat["name"]
            thumb_dir = base_dir / "thumbnails"
            thumb_dir.mkdir(parents=True, exist_ok=True)

            # 📍 카테고리가 시작될 때 상태를 한 번 초기화 (이후 멀티 URL이 이 상태를 공유함)
            status.update({
                "cat_name": cat["name"],
                "thumb_path": str(thumb_dir),
                "json_path": str(base_dir / f"{cat['name']}_data.json"),
                "seen_ids": set(), # URL 1과 URL 2 사이의 중복 방지
                "current_list": []
            })

            print(f"\n📂 작업 시작 카테고리: {cat['name'].upper()}")

            # ⭐ [핵심 추가] 카테고리 내부의 각 URL 순회
            for idx, url in enumerate(cat["urls"], 1):
                print(f"🔗 소스 {idx} 수집 중: {url[:60]}...")
                
                for r in range(5): # URL당 새로고침 5회 유지
                    print(f"   🔄 로딩 중... ({r+1}/5)")
                    try:
                        await page.goto(url, wait_until="domcontentloaded", timeout=60000)
                        await asyncio.sleep(5)

                        for s in range(15): # URL당 심층 스크롤 15회 유지
                            await page.mouse.wheel(0, 5000)
                            await asyncio.sleep(random.uniform(3, 5))
                    except Exception as e:
                        print(f"   ⚠️ 소스 {idx} 타임아웃 발생(무시): {e}")

            # 💾 카테고리의 모든 URL 수집이 끝나면 한 번에 저장
            with open(status["json_path"], "w", encoding="utf-8") as f:
                json.dump(status["current_list"], f, ensure_ascii=False, indent=2)
            print(f"💾 {cat['name']} 카테고리 통합 완료 ({len(status['current_list'])}건)")

        print(f"\n✨ [{TODAY}] 모든 카테고리 및 멀티 소스 크롤링 완수!")
        await context.close()

if __name__ == "__main__":
    initialize_folder()
    asyncio.run(run_intercept())