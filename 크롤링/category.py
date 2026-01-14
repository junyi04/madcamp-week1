from fastapi import APIRouter
import os
import json
from datetime import datetime

router = APIRouter()

# 🔻 [수정 1] IP 대신 main.py와 동일한 도메인 입력 (http/https 구분 주의)
# SERVER_IP = "10.249.86.17"  <-- 이 줄 지우고 아래로 변경
SERVER_DOMAIN = "young-forty.ngrok.app" 
SERVER_PROTOCOL = "https" # ngrok은 보통 https 사용 (로컬 테스트시엔 http)
MYSQL_CONFIG = {
    'host': 'localhost',
    'database': 'madcamp1_db',
    'user': 'root',
    'password': '4038'  # 변경!
}
@router.get("/category/{category_name}")
def get_category_data(category_name: str):
  today = datetime.now().strftime("%Y-%m-%d")
  JSON_PATH = f"{today}/{category_name}/top10/{category_name}_top10.json"
  
  if os.path.exists(JSON_PATH):
    try:
      with open(JSON_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
        for item in data:
          raw_path = item.get('image_file', '')
          clean_path = raw_path.lstrip('/') 
          
          # 🔻 [수정 2] 포트(:8001) 제거하고 도메인 방식으로 변경
          # item['imageFile'] = f"http://{SERVER_IP}:8001/{clean_path}" <-- 지우고 아래로 변경
          item['imageFile'] = f"{SERVER_PROTOCOL}://{SERVER_DOMAIN}/{clean_path}"
          
          print(f"생성된 이미지 URL: {item['imageFile']}")
        return data
    except Exception as e:
      return {"error": f"JSON 에러: {str(e)}"}
          
  return {"error": f"파일을 찾을 수 없습니다: {JSON_PATH}"}