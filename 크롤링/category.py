from fastapi import APIRouter, Request
import os
import json
from datetime import datetime

router = APIRouter()

@router.get("/category/{category_name}")
def get_category_data(category_name: str, request: Request):
  today = datetime.now().strftime("%Y-%m-%d")
  JSON_PATH = f"{today}/{category_name}/top10/{category_name}_top10.json"
  
  base_url = str(request.base_url).rstrip('/')
  
  if os.path.exists(JSON_PATH):
    try:
      with open(JSON_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
        for item in data:
          raw_path = item.get('image_file', '')
          clean_path = raw_path.lstrip('/')
          
          item['imageFile'] = f"{base_url}/{clean_path}"
          
          print(f"생성된 이미지 URL: {item['imageFile']}")
        return data
    except Exception as e:
      return {"error": f"JSON 에러: {str(e)}"}
          
  return {"error": f"파일을 찾을 수 없습니다: {JSON_PATH}"}