from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
import os
import json
import category
from datetime import datetime

app = FastAPI()
SERVER_IP = "10.249.86.17"

app.include_router(category.router)
app.mount("/static", StaticFiles(directory="."), name="static")

@app.get("/top10")
def get_main_data():
  print("!!! 서버에 요청이 들어왔습니다 !!!")
  today = datetime.now().strftime("%Y-%m-%d")
  JSON_PATH = f"{today}/main/top10/main_top10.json"
  
  print(f"접속 시도 경로: {JSON_PATH}")
  
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