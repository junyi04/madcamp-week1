from fastapi import APIRouter
import os
import json
from datetime import datetime

router = APIRouter()
SERVER_IP = "10.249.86.17"

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
          item['imageFile'] = f"http://{SERVER_IP}:8001/{clean_path}"
          
          print(f"생성된 이미지 URL: {item['imageFile']}")
        return data
    except Exception as e:
      return {"error": f"JSON 에러: {str(e)}"}
          
  return {"error": f"파일을 찾을 수 없습니다: {JSON_PATH}"}