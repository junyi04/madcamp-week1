import asyncio
import json
import random
import os
import requests
from datetime import datetime
from playwright.async_api import async_playwright

# --- 🚀 설정 영역 ---
# 1. 5개 카테고리 설정
CATEGORIES = [
    {"name": "main", "url": "https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%80-%EC%88%9C%EC%9C%BC%EB%A1%9C-%EC%A0%95%EB%A0%AC"},
    {"name": "dance", "url": "https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%EC%B6%A4-2025"},
    {"name": "challenge", "url": "https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%ED%8B%B1%ED%86%A1-%EC%B1%8C%EB%A6%B0%EC%A7%80"},
    {"name": "food", "url": "https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EB%A8%B9%EB%B0%A9%EC%A1%B0%ED%9A%8C%EC%88%98-1%EC%9C%84-%EC%98%81%EC%83%81"},
    {"name": "tts", "url": "https://www.tiktok.com/discover/%EC%9D%8C%EC%84%B1%EB%B3%80%EC%A1%B0-%EC%88%9C%EC%9C%84-%ED%8B%B1%ED%86%A1%EC%BB%A4"}
]

# 📍 보안사항 반영: 날짜별 폴더 생성
TODAY = datetime.now().strftime("%Y-%m-%d")
USER_DATA_DIR = f"C:/Users/{os.getlogin()}/AppData/Local/Google/Chrome/no_mor09"
# --------------------

def download_image_sync(url, save_path):
    if os.path.exists(save_path): return True
    try:
        response = requests.get(url, timeout=5)
        if response.status_code == 200:
            with open(save_path, 'wb') as f:
                f.write(response.content)
            return True
    except: return False
    return False

async def run_intercept():
    async with async_playwright() as p:
        # 로그인 세션 유지 브라우저 실행
        context = await p.chromium.launch_persistent_context(
            user_data_dir=USER_DATA_DIR,
            channel="chrome",
            headless=False,
            args=["--disable-blink-features=AutomationControlled"],
            slow_mo=50
        )
        page = await context.new_page()

        # 실시간 수집 상태 관리
        status = {
            "cat_name": "", 
            "thumb_path": "", 
            "json_path": "", 
            "seen_ids": set()
        }

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

                            # 📍 보안사항 반영: 100k(10만) 조회수 필터링
                            if v_id and v_id not in status["seen_ids"] and play_count >= 100000:
                                author = item.get('author', {})
                                img_url = item.get('video', {}).get('cover') or item.get('video', {}).get('originCover')
                                if not img_url: continue

                                # 📍 보안사항 반영: 원본 ID 유지 (나중에 정렬 코드에서 커스텀 예정)
                                img_path = os.path.join(status["thumb_path"], f"{v_id}.jpg")
                                
                                success = await asyncio.to_thread(download_image_sync, img_url, img_path)

                                if success:
                                    info = {
                                        "id": v_id,  # 원본 ID 사용
                                        "title": item.get('desc', '제목 없음'),
                                        "author": author.get('nickname', '알 수 없음'),
                                        "views": play_count,
                                        "likes": item.get('stats', {}).get('diggCount', 0),
                                        "url": f"https://www.tiktok.com/@{author.get('uniqueId')}/video/{v_id}",
                                        "image_file": img_path.replace("\\", "/")
                                    }
                                    with open(status["json_path"], "a", encoding="utf-8") as f:
                                        f.write(json.dumps(info, ensure_ascii=False) + "\n")
                                    
                                    status["seen_ids"].add(v_id)
                                    print(f"   [포착] ID: {v_id} | 조회수: {play_count:,}")
                    except: pass

        page.on("response", handle_response)

        for cat in CATEGORIES:
            # 폴더 구조: 날짜 / 카테고리 / thumbnails
            base_dir = os.path.join(TODAY, cat["name"])
            thumb_dir = os.path.join(base_dir, "thumbnails")
            os.makedirs(thumb_dir, exist_ok=True)

            status["cat_name"] = cat["name"]
            status["thumb_path"] = thumb_dir
            status["json_path"] = os.path.join(base_dir, f"{cat['name']}_data.json")
            status["seen_ids"] = set()

            print(f"\n📂 [{TODAY}] 카테고리 작업 중: {cat['name'].upper()}")

            for r in range(5): # 새로고침 5회
                print(f"🔄 새로고침 {r+1}/5... (신규 데이터를 로딩합니다)")
                await page.goto(cat["url"], wait_until="domcontentloaded", timeout=60000)
                await asyncio.sleep(5)

                # 📍 보안사항 반영: 필터링 손실을 막기 위해 스크롤 횟수를 15회로 증설
                for s in range(15): 
                    print(f"   🖱️ 심층 스크롤 중... ({s+1}/15)")
                    await page.mouse.wheel(0, 5000)
                    await asyncio.sleep(random.uniform(3, 5))

        print(f"\n✨ [{TODAY}] 전 카테고리 수집 완료!")
        await context.close()

if __name__ == "__main__":
    asyncio.run(run_intercept())