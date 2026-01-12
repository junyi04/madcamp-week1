import os
import json
import torch
import torch.nn as nn
import torchvision.models as models
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
import shutil
import cv2
from pathlib import Path
from tqdm import tqdm
from datetime import datetime
from ultralytics import YOLO
from facenet_pytorch import MTCNN, InceptionResnetV1
from sklearn.metrics.pairwise import cosine_similarity

# ===========================
# 🎛️ RTX 4060 환경 최적화 설정
# ===========================
SIMILARITY_THRESHOLD = 0.85 
W_COSINE = 0.7              # ResNet 3-Pass (기존 기능) 가중치
W_FACE = 0.3                # 얼굴 유사도 가중치
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
BATCH_SIZE = 16             # 4060 VRAM(8GB) 고려
QUARANTINE_FOLDER = "duplicates_storage" 
QUARANTINE_JSON_LOG = os.path.join(QUARANTINE_FOLDER, "quarantined_json_data.json")

# 📍 [경로 에러 해결] 모델 저장 경로를 C 드라이브 프로젝트 폴더로 강제 지정
project_root = Path.cwd()
model_cache_dir = project_root / "models_cache"
model_cache_dir.mkdir(parents=True, exist_ok=True)
os.environ['TORCH_HOME'] = str(model_cache_dir) # 환경변수 설정
torch.hub.set_dir(str(model_cache_dir))        # Hub 경로 설정
# ===========================

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 1. 모델 로드 (기존 ResNet 3-Pass 기능 포함)
# weights_only=False 설정을 통해 가중치 로드 에러 방지
resnet50 = models.resnet50(weights=models.ResNet50_Weights.DEFAULT)
resnet50 = nn.Sequential(*list(resnet50.children())[:-1]).to(device).eval()

yolo = YOLO('yolov8n.pt') 
mtcnn = MTCNN(keep_all=False, device=device) 
facenet = InceptionResnetV1(pretrained='vggface2').to(device).eval()

# 🎨 [기능 유지] 3-Pass 전처리 (Center, Full, Zoom)
preprocess = {
    "center": transforms.Compose([transforms.Resize(256), transforms.CenterCrop(224), transforms.ToTensor(), transforms.Normalize([0.485,0.456,0.406],[0.229,0.224,0.225])]),
    "full": transforms.Compose([transforms.Resize((224, 224)), transforms.ToTensor(), transforms.Normalize([0.485,0.456,0.406],[0.229,0.224,0.225])]),
    "zoom": transforms.Compose([transforms.Resize(400), transforms.CenterCrop(224), transforms.ToTensor(), transforms.Normalize([0.485,0.456,0.406],[0.229,0.224,0.225])])
}

def get_face_embedding(img_p):
    try:
        img = Image.open(img_p).convert('RGB')
        face = mtcnn(img)
        if face is not None:
            return facenet(face.unsqueeze(0).to(device)).detach().cpu().numpy(), True
    except: pass
    return None, False

def move_and_visualize(src_path, dest_path, item):
    """YOLO 박스 시각화 후 격리 이동"""
    os.makedirs(QUARANTINE_FOLDER, exist_ok=True)
    results = yolo(src_path, verbose=False)
    img = cv2.imread(str(src_path))
    for r in results:
        for box in r.boxes:
            if box.cls == 0: # person 탐지 시
                b = box.xyxy[0].cpu().numpy().astype(int)
                cv2.rectangle(img, (b[0], b[1]), (b[2], b[3]), (0, 255, 0), 3)
                cv2.putText(img, "AI DUPLICATE", (b[0], b[1]-10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    cv2.imwrite(str(dest_path), img)
    if os.path.exists(src_path): os.remove(src_path)
    with open(QUARANTINE_JSON_LOG, "a", encoding="utf-8") as f:
        f.write(json.dumps(item, ensure_ascii=False) + "\n")

if __name__ == "__main__":
    print(f"🚀 [RTX 4060] AI 정밀 정제 시작 | 대상: {TARGET_DATE_FOLDER}")
    
    # 이미지 수집
    all_paths = [str(p) for p in Path(TARGET_DATE_FOLDER).glob("**/thumbnails/*.jpg")]
    if len(all_paths) < 2: 
        print("✅ 분석할 데이터가 부족합니다."); exit()

    valid_paths, res_feats, face_feats = [], [], {}
    for p in tqdm(all_paths, desc="🧠 3-Pass 분석 중"):
        try:
            img = Image.open(p).convert('RGB')
            # ⭐ [기능 유지] ResNet 3-Pass TTA
            with torch.no_grad():
                f_avg = (resnet50(preprocess["center"](img).unsqueeze(0).to(device)) +
                         resnet50(preprocess["full"](img).unsqueeze(0).to(device)) +
                         resnet50(preprocess["zoom"](img).unsqueeze(0).to(device))) / 3.0
                res_feats.append(torch.flatten(f_avg, 1).cpu().numpy())
            
            emb, ok = get_face_embedding(p)
            if ok: face_feats[p] = emb
            valid_paths.append(p)
        except: continue

    res_feats = np.vstack(res_feats)
    res_sim = cosine_similarity(res_feats)

    deleted_indices = set()
    deleted_info_by_cat = {}

    for i in range(len(valid_paths)):
        if i in deleted_indices: continue
        for j in range(i + 1, len(valid_paths)):
            if j in deleted_indices: continue
            
            s_res = res_sim[i][j]
            pi, pj = valid_paths[i], valid_paths[j]
            
            # ⭐ [가중치 7:3 반영]
            if pi in face_feats and pj in face_feats:
                s_face = cosine_similarity(face_feats[pi], face_feats[pj])[0][0]
                final_score = (s_res * W_COSINE) + (s_face * W_FACE)
            else:
                final_score = s_res

            if final_score >= SIMILARITY_THRESHOLD:
                deleted_indices.add(j)
                p_path = Path(pj)
                cat = p_path.parent.parent.name
                if cat not in deleted_info_by_cat: deleted_info_by_cat[cat] = []
                deleted_info_by_cat[cat].append(p_path.stem)

    print(f"📦 {len(deleted_indices)}개 중복 격리 및 JSON 동기화 진행")
    for idx in sorted(list(deleted_indices), reverse=True):
        p_path = valid_paths[idx]
        dest = Path(QUARANTINE_FOLDER) / f"DUP_{Path(p_path).name}"
        category = Path(p_path).parent.parent.name
        move_and_visualize(p_path, dest, {"cat": category, "id": Path(p_path).stem})

    # JSON 업데이트 및 제목 없음 보정
    for cat, d_ids in deleted_info_by_cat.items():
        j_path = Path(TARGET_DATE_FOLDER) / cat / f"{cat}_data.json"
        if j_path.exists():
            with open(j_path, "r", encoding="utf-8") as f:
                data_list = json.load(f)
            
            new_list = []
            for item in data_list:
                if item.get("id") not in d_ids:
                    if not item.get('title') or item['title'].strip() == "":
                        item['title'] = "제목 없음"
                    new_list.append(item)
            
            with open(j_path, "w", encoding="utf-8") as f:
                json.dump(new_list, f, ensure_ascii=False, indent=2)

    print("🎉 정제 작업 완료.")