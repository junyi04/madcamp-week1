import os
import json
import torch
import torch.nn as nn
import torchvision.models as models
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
import shutil
from pathlib import Path
from tqdm import tqdm
from datetime import datetime

# ===========================
# 🎛️ 사용자 파라미터 조절 영역
# ===========================
SIMILARITY_THRESHOLD = 0.82  # 정밀도가 높아졌으므로 0.82~0.85 추천
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
BATCH_SIZE = 256 # 3중 연산이므로 512보다 낮춰서 안정성 확보
QUARANTINE_FOLDER = "duplicates_storage" 
QUARANTINE_JSON_LOG = os.path.join(QUARANTINE_FOLDER, "quarantined_json_data.json")
# ===========================

# 모델 설정 (프로젝트 폴더 내 저장)
project_root = Path.cwd()
model_dir = project_root / "models"
model_dir.mkdir(parents=True, exist_ok=True)
torch.hub.set_dir(str(model_dir))

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = models.resnet50(weights=models.ResNet50_Weights.DEFAULT)
model = nn.Sequential(*list(model.children())[:-1]) 
model.to(device).eval()

# ⭐ [정밀 분석] 3가지 다른 시점의 전처리 정의
# 1. 중앙 집중 (Center Crop)
transform_center = transforms.Compose([
    transforms.Resize(256), transforms.CenterCrop(224), transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])
# 2. 전체 구도 (Full View)
transform_full = transforms.Compose([
    transforms.Resize((224, 224)), transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])
# 3. 확대 분석 (Detailed Zoom)
transform_zoom = transforms.Compose([
    transforms.Resize(400), transforms.CenterCrop(224), transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

def extract_features_multi_pass(image_paths, model, device, batch_size):
    """한 이미지당 3번의 특징을 추출하여 평균을 냅니다."""
    final_features = []
    valid_paths = []
    
    for i in tqdm(range(0, len(image_paths), batch_size), desc="🔍 이미지 3중 정밀 분석 중"):
        batch_files = image_paths[i:i+batch_size]
        
        # 각 전처리 방식별 특징 보관용
        batch_f1, batch_f2, batch_f3 = [], [], []
        
        for p in batch_files:
            try:
                img = Image.open(p).convert('RGB')
                # 3가지 시점으로 이미지 변환
                batch_f1.append(transform_center(img))
                batch_f2.append(transform_full(img))
                batch_f3.append(transform_zoom(img))
                valid_paths.append(p)
            except: continue
            
        if not batch_f1: continue
        
        with torch.no_grad():
            # 3번의 분석 실행
            t1 = torch.stack(batch_f1).to(device)
            t2 = torch.stack(batch_f2).to(device)
            t3 = torch.stack(batch_f3).to(device)
            
            out1 = torch.flatten(model(t1), 1).cpu().numpy()
            out2 = torch.flatten(model(t2), 1).cpu().numpy()
            out3 = torch.flatten(model(t3), 1).cpu().numpy()
            
            # ⭐ [중요] 3개 벡터의 평균을 내어 '강력한 특징' 생성
            # $$ \mathbf{f}_{final} = \frac{\mathbf{f}_{center} + \mathbf{f}_{full} + \mathbf{f}_{zoom}}{3} $$
            avg_features = (out1 + out2 + out3) / 3.0
            final_features.append(avg_features)
            
    return valid_paths, np.vstack(final_features) if final_features else ([], np.array([]))

def move_to_quarantine(image_path_str):
    """이미지 격리 및 JSON 백업 로직"""
    try:
        os.makedirs(QUARANTINE_FOLDER, exist_ok=True)
        img_p = Path(image_path_str)
        filename = img_p.name
        category_dir = img_p.parent.parent 
        category_name = category_dir.name
        json_file_path = category_dir / f"{category_name}_data.json"
        
        # 파일 이동 (카테고리명_ID.jpg)
        dest_filename = f"{category_name}_{filename}"
        dest_path = os.path.join(QUARANTINE_FOLDER, dest_filename)
        shutil.move(image_path_str, dest_path)

        # JSON 정제
        target_id = img_p.stem 
        if json_file_path.exists():
            temp_lines = []
            quarantined_data = None
            with open(json_file_path, "r", encoding="utf-8") as f:
                for line in f:
                    try:
                        data = json.loads(line)
                        if data.get("id") == target_id:
                            quarantined_data = data
                            continue
                        temp_lines.append(line)
                    except: temp_lines.append(line)
            
            with open(json_file_path, "w", encoding="utf-8") as f:
                f.writelines(temp_lines)
            
            if quarantined_data:
                quarantined_data["quarantined_at"] = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                with open(QUARANTINE_JSON_LOG, "a", encoding="utf-8") as f:
                    f.write(json.dumps(quarantined_data, ensure_ascii=False) + "\n")
    except Exception as e:
        print(f"⚠️ 격리 실패: {e}")

if __name__ == "__main__":
    print(f"🚀 [3중 분석] AI 중복 제거기 가동 | 대상: {TARGET_DATE_FOLDER}")
    
    # 1. 이미지 수집 (날짜/카테고리/thumbnails/*.jpg)
    all_image_paths = [str(p) for p in Path(TARGET_DATE_FOLDER).glob("**/thumbnails/*.jpg")]
    print(f"📸 수집된 이미지: {len(all_image_paths)}개")
    
    if len(all_image_paths) < 2:
        print("✅ 분석할 이미지가 없습니다."); exit()

    # 2. 3PASS 특징 추출
    valid_paths, features = extract_features_multi_pass(all_image_paths, model, device, BATCH_SIZE)

    # 3. 코사인 유사도 계산
    from sklearn.preprocessing import normalize
    features = normalize(features, axis=1) 
    sim_matrix = np.matmul(features, features.T) 

    # 4. 중복 판별
    deleted_indices = set()
    for i in range(len(valid_paths)):
        if i in deleted_indices: continue
        for j in range(i + 1, len(valid_paths)):
            if j not in deleted_indices and sim_matrix[i][j] >= SIMILARITY_THRESHOLD:
                deleted_indices.add(j)

    # 5. 격리 실행
    print(f"\n📦 {len(deleted_indices)}개의 중복 의심 데이터를 격리합니다.")
    for idx in sorted(list(deleted_indices), reverse=True):
        move_to_quarantine(valid_paths[idx])

    print(f"🎉 3중 정밀 분석 및 정제가 완료되었습니다.")