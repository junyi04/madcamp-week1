import os
import json
import shutil
import re
from pathlib import Path
from google.cloud import translate_v2 as translate
from tqdm import tqdm
from datetime import datetime

# ===========================
# 🎛️ 설정 영역
# ===========================
os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = "google_key.json"
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
QUARANTINE_FOLDER = "non_korean_quarantine"
CLEANUP_THRESHOLD = 1000 

translate_client = translate.Client()
# ===========================

def is_korean_ai(text):
    """구글 API를 사용하여 텍스트의 한국어 여부를 판별합니다."""
    if not text or text.strip() == "":
        return False
    
    # [비용 절감] 한글이 최소 한 글자라도 포함되어 있어야 API 호출
    if not re.search('[가-힣]', text):
        return False

    try:
        result = translate_client.detect_language(text)
        if result['language'] == 'ko':
            return True
    except Exception as e:
        print(f"⚠️ API 에러: {e}")
        return len(re.sub('[^가-힣]', '', text)) > 0
    return False

if __name__ == "__main__":
    print(f"🚀 [이중 체크] NLP 필터링 시작 | 대상: {TARGET_DATE_FOLDER}")
    
    base_path = Path(TARGET_DATE_FOLDER)
    json_files = list(base_path.glob("**/*_data.json"))
    
    for json_path in json_files:
        cat_name = json_path.parent.name
        temp_lines = []
        removed_count = 0
        
        with open(json_path, "r", encoding="utf-8") as f:
            lines = f.readlines()

        for line in tqdm(lines, desc=f"🌍 [{cat_name.upper()}] 분석 중"):
            try:
                data = json.loads(line)
                title = data.get("title", "").strip()
                author_nickname = data.get("author", "").strip()
                img_file = data.get("image_file")

                # ⭐ [핵심 로직] 판단 대상 결정
                # 제목이 있으면 제목으로, 없으면 작성자 닉네임으로 판단
                target_text = title if title else author_nickname
                
                if is_korean_ai(target_text):
                    temp_lines.append(line)
                else:
                    # 격리소 이동 로직 (이전과 동일)
                    os.makedirs(QUARANTINE_FOLDER, exist_ok=True)
                    img_p = Path(img_file)
                    dest_path = os.path.join(QUARANTINE_FOLDER, f"nonko_{cat_name}_{img_p.name}")
                    if img_p.exists():
                        shutil.move(img_file, dest_path)
                    
                    data["quarantined_reason"] = f"Non-Korean (Checked: {'Title' if title else 'Author'})"
                    with open(os.path.join(QUARANTINE_FOLDER, "non_korean_data.json"), "a", encoding="utf-8") as lf:
                        lf.write(json.dumps(data, ensure_ascii=False) + "\n")
                    removed_count += 1
            except:
                temp_lines.append(line)

        with open(json_path, "w", encoding="utf-8") as f:
            f.writelines(temp_lines)
        
        print(f"   ✅ {cat_name}: {removed_count}개 제외 완료")

    print(f"🎉 이중 체크 필터링이 완료되었습니다.")