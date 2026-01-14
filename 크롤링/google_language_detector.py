import os
import json
import shutil
import re
from pathlib import Path
from google.cloud import translate_v2 as translate
from tqdm import tqdm
from datetime import datetime
import mysql.connector
from mysql.connector import Error

MYSQL_CONFIG = {
    'host': 'localhost',
    'database': 'madcamp1_db',
    'user': 'root',
    'password': '4038'  # 비밀번호 변경
}

SERVER_DOMAIN = "young-forty.ngrok.app"

def save_filtered_to_mysql(filtered_data, category, filtered_date):
    """한국어 아닌 것을 MySQL에 저장"""
    try:
        connection = mysql.connector.connect(**MYSQL_CONFIG)
        cursor = connection.cursor()
        
        sql = """INSERT INTO filtered_non_korean 
                 (id, original_id, title, author, views, likes, category, url, 
                  image_url, detected_language, filtered_date) 
                 VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                 ON DUPLICATE KEY UPDATE 
                 title=VALUES(title), views=VALUES(views)"""
        
        for item in filtered_data:
            # 이미지 URL 변환
            img_path = item.get('image_file', '')
            if img_path:
                img_name = Path(img_path).name
                local_path = f"{filtered_date}/{category}/thumbnails/{img_name}"
                image_url = f"https://{SERVER_DOMAIN}/{local_path}"
            else:
                image_url = None
            
            cursor.execute(sql, (
                f"nonko_{category}_{item.get('id')}",
                item.get('id'),
                item.get('title', ''),
                item.get('author', ''),
                item.get('views', 0),
                item.get('likes', 0),
                category,
                item.get('url', ''),
                image_url,
                'unknown',  # 추후 감지된 언어 저장 가능
                filtered_date
            ))
        
        connection.commit()
        print(f"   💾 MySQL 저장: {category} 한국어 아닌 것 {len(filtered_data)}건")
        
    except Error as e:
        print(f"   ❌ MySQL 에러: {e}")
    finally:
        if connection and connection.is_connected():
            cursor.close()
            connection.close()
# ===========================
# 🎛️ 설정 영역
# ===========================
os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = "google_key.json"
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
QUARANTINE_FOLDER = "non_korean_quarantine"

translate_client = translate.Client()
# ===========================

def is_korean_ai(text):
    """구글 API를 사용하되, 짧은 텍스트 및 한글 포함 여부를 우선 순위로 둡니다."""
    if not text or text.strip() == "":
        return False
    
    # 1. 한글이 포함되어 있는지 먼저 체크 (정규식)
    korean_chars = re.sub('[^가-힣]', '', text)
    if len(korean_chars) > 0:
        # 한글이 2글자 이상 포함되어 있다면 일단 한국어로 간주 (API 비용 절감 및 유실 방지)
        if len(korean_chars) >= 2:
            return True
            
        # 한글이 1글자만 있는 경우 API로 정밀 검사
        try:
            result = translate_client.detect_language(text)
            if result['language'] == 'ko':
                return True
        except:
            return False
    
    return False

if __name__ == "__main__":
    print(f"🚀 [NLP 이중 검증] 필터링 시작 | 대상: {TARGET_DATE_FOLDER}")
    
    base_path = Path(TARGET_DATE_FOLDER)
    # _data.json 파일들만 탐색 (top10 폴더 제외)
    json_files = [p for p in base_path.glob("**/*_data.json") if "top10" not in str(p)]
    
    for json_path in json_files:
        cat_name = json_path.parent.name
        filtered_data = []
        removed_count = 0
        removed_data = []  # ⭐ 추가: 제거된 데이터 저장

        try:
            with open(json_path, "r", encoding="utf-8") as f:
                # ⭐ [핵심 수정] 표준 JSON 배열 형식을 리스트로 읽기
                all_data = json.load(f)
        except Exception as e:
            print(f"⚠️ {json_path} 읽기 실패: {e}")
            continue

        for data in tqdm(all_data, desc=f"🌍 [{cat_name.upper()}] 분석 중"):
            title = data.get("title", "").strip()
            author = data.get("author", "").strip()
            img_file = data.get("image_file")

            # ⭐ [로직 수정] 제목 혹은 작성자 중 하나라도 한국어라면 유지
            # 제목에 한글이 없어도 작성자가 한국인이면 한국 릴스일 확률이 매우 높음
            is_korean = is_korean_ai(title) or is_korean_ai(author)

            if is_korean:
                filtered_data.append(data)
            else:
                removed_data.append(data)
                # 격리소 이동 로직
                os.makedirs(QUARANTINE_FOLDER, exist_ok=True)
                img_p = Path(img_file)
                # 실제 이미지 경로가 category 폴더 안에 있는지 확인
                real_img_path = json_path.parent / img_p.name if not img_p.exists() else img_p
                
                if real_img_path.exists():
                    dest_path = os.path.join(QUARANTINE_FOLDER, f"nonko_{cat_name}_{real_img_path.name}")
                    shutil.move(str(real_img_path), dest_path)
                
                removed_count += 1

        # ⭐ [핵심 수정] 필터링된 결과물도 다시 표준 배열 형식으로 저장
        with open(json_path, "w", encoding="utf-8") as f:
            json.dump(filtered_data, f, ensure_ascii=False, indent=2)
        if removed_data:
            save_filtered_to_mysql(removed_data, cat_name, TARGET_DATE_FOLDER)
        
        print(f"   ✅ {cat_name}: {removed_count}개 제외 완료")

    print(f"🎉 모든 필터링이 완료되었습니다.")