import asyncio
import json
import random
import os
import requests
import shutil
from datetime import datetime
from pathlib import Path  # 경로 처리를 위해 추가
from playwright.async_api import async_playwright

# --- 🚀 설정 영역 ---
CATEGORIES = [
    {"name": "main", "url": "https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%80-%EC%88%9C%EC%9C%BC%EB%A1%9C-%EC%A0%95%EB%A0%AC"},
    {"name": "dance", "url": "https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%EC%B6%A4-2025"},
    {"name": "challenge", "url": "https://www.tiktok.com/discover/%EC%A1%B0%ED%9A%8C%EC%88%98-%EB%A7%8E%EC%9D%B4-%EB%82%98%EC%98%A4%EB%8A%94-%ED%8B%B1%ED%86%A1-%EC%B1%8C%EB%A6%B0%EC%A7%80"},
    {"name": "food", "url": "https://www.tiktok.com/discover/%ED%8B%B1%ED%86%A1-%EB%A8%B9%EB%B0%A9%EC%A1%B0%ED%9A%8C%EC%88%98-1%EC%9C%84-%EC%98%81%EC%83%81"},
    {"name": "tts", "url": "https://www.tiktok.com/discover/%EC%9D%8C%EC%84%B1%EB%B3%80%EC%A1%B0-%EC%88%9C%EC%9C%84-%ED%8B%B1%ED%86%A1%BB%A4"}
]

TODAY = datetime.now().strftime("%Y-%m-%d")
USER_DATA_DIR = f"C:/Users/{os.getlogin()}/AppData/Local/Google/Chrome/no_mor09"

# 📍 [수정] 실행 전 폴더 초기화 (서버 점유로 인한 삭제 실패 예외 처리 추가)
def initialize_folder():
    if os.path.exists(TODAY):
        try:
            print(f"🧹 오늘자({TODAY}) 기존 폴더를 정리합니다.")
            shutil.rmtree(TODAY)
        except PermissionError:
            print(f"⚠️ 경고: 서버가 폴더를 사용 중입니다. 삭제 대신 덮어쓰기를 진행합니다.")
    os.makedirs(TODAY, exist_ok=True)

initialize_folder()
# --------------------

def download_image_sync(url, save_path):
    try:
        # 폴더가 없으면 생성
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
            print(f"❌ 브라우저 실행 실패: {e}\n💡 모든 크롬 창을 닫고 다시 실행하세요.")
            return

        page = await context.new_page()

        # 실시간 상태 관리
        status = {
            "cat_name": "", 
            "thumb_path": "", 
            "json_path": "", 
            "seen_ids": set(),
            "current_list": [] 
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

                            if v_id and v_id not in status["seen_ids"] and play_count >= 100000:
                                author = item.get('author', {})
                                img_url = item.get('video', {}).get('cover') or item.get('video', {}).get('originCover')
                                if not img_url: continue

                                # 썸네일 경로 설정
                                img_path = os.path.join(status["thumb_path"], f"{v_id}.jpg")
                                success = await asyncio.to_thread(download_image_sync, img_url, img_path)

                                if success:
                                    info = {
                                        "id": v_id,
                                        "title": item.get('desc', '제목 없음'),
                                        "author": author.get('nickname', '알 수 없음'),
                                        "views": play_count,
                                        "likes": item.get('stats', {}).get('diggCount', 0),
                                        "category": status["cat_name"],
                                        "url": f"https://www.tiktok.com/@{author.get('uniqueId')}/video/{v_id}",
                                        "image_file": img_path.replace("\\", "/")
                                    }
                                    status["current_list"].append(info)
                                    status["seen_ids"].add(v_id)
                                    print(f"   [포착] {status['cat_name']} | ID: {v_id} | 조회수: {play_count:,}")
                    except: pass

        page.on("response", handle_response)

        for cat in CATEGORIES:
            # ⭐ [핵심 수정] 폴더 경로를 생성할 때 부모 폴더까지 강제 생성
            base_dir = Path(TODAY) / cat["name"]
            thumb_dir = base_dir / "thumbnails"
            thumb_dir.mkdir(parents=True, exist_ok=True)

            status["cat_name"] = cat["name"]
            status["thumb_path"] = str(thumb_dir)
            status["json_path"] = str(base_dir / f"{cat['name']}_data.json")
            status["seen_ids"] = set()
            status["current_list"] = []

            print(f"\n📂 카테고리 수집 시작: {cat['name'].upper()}")

            for r in range(5): 
                print(f"🔄 새로고침 {r+1}/5...")
                try:
                    await page.goto(cat["url"], wait_until="domcontentloaded", timeout=60000)
                    await asyncio.sleep(5)

                    for s in range(15): 
                        await page.mouse.wheel(0, 5000)
                        await asyncio.sleep(random.uniform(3, 5))
                except:
                    print(f"⚠️ {cat['name']} 로딩 중 타임아웃 발생, 다음 시도로 넘어갑니다.")

            # ⭐ [표준 JSON 배열 저장]
            if status["current_list"]:
                with open(status["json_path"], "w", encoding="utf-8") as f:
                    json.dump(status["current_list"], f, ensure_ascii=False, indent=2)
                print(f"💾 {cat['name']} 저장 완료: {len(status['current_list'])}건")
            else:
                # 데이터가 없더라도 빈 리스트 파일 생성 (에러 방지)
                with open(status["json_path"], "w", encoding="utf-8") as f:
                    json.dump([], f, ensure_ascii=False, indent=2)

        print(f"\n✨ [{TODAY}] 모든 카테고리 원본 수집 완료!")
        await context.close()

if __name__ == "__main__":
    asyncio.run(run_intercept())