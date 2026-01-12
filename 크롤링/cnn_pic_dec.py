import os
import json
import torch
import torch.nn as nn
import torchvision.models as models
import torchvision.transforms as transforms
from PIL import Image
import numpy as np
import cv2
from pathlib import Path
from tqdm import tqdm
from datetime import datetime
from facenet_pytorch import MTCNN, InceptionResnetV1
from sklearn.metrics.pairwise import cosine_similarity

# ===========================
# 🎛️ RTX 4060 환경 및 정밀도 설정
# ===========================
SIMILARITY_THRESHOLD = 0.92  # 틱톡 배경 중복 방지를 위해 약간 상향
W_COSINE = 0.3               # ResNet 3-Pass 가중치 (50%)
W_FACE = 0.7                 # FaceNet 얼굴 가중치 (50%)
TARGET_DATE_FOLDER = datetime.now().strftime("%Y-%m-%d") 
QUARANTINE_FOLDER = "duplicates_storage" 
QUARANTINE_JSON_LOG = os.path.join(QUARANTINE_FOLDER, "quarantined_json_data.json")

# 경로 에러 방지 설정
project_root = Path.cwd()
model_cache_dir = project_root / "models_cache"
model_cache_dir.mkdir(parents=True, exist_ok=True)
os.environ['TORCH_HOME'] = str(model_cache_dir)
torch.hub.set_dir(str(model_cache_dir))
# ===========================

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# 1. 모델 로드 (FaceNet/MTCNN 기반 시각화 준비)
resnet50 = models.resnet50(weights=models.ResNet50_Weights.DEFAULT)
resnet50 = nn.Sequential(*list(resnet50.children())[:-1]).to(device).eval()

# MTCNN: 얼굴 좌표(Box) 추출을 위해 post_process=False 유지
mtcnn = MTCNN(keep_all=False, device=device, post_process=False) 
facenet = InceptionResnetV1(pretrained='vggface2').to(device).eval()

# 🎨 3-Pass 전처리 (기능 유지 100%)
preprocess = {
    "center": transforms.Compose([transforms.Resize(256), transforms.CenterCrop(224), transforms.ToTensor(), transforms.Normalize([0.485,0.456,0.406],[0.229,0.224,0.225])]),
    "full": transforms.Compose([transforms.Resize((224, 224)), transforms.ToTensor(), transforms.Normalize([0.485,0.456,0.406],[0.229,0.224,0.225])]),
    "zoom": transforms.Compose([transforms.Resize(400), transforms.CenterCrop(224), transforms.ToTensor(), transforms.Normalize([0.485,0.456,0.406],[0.229,0.224,0.225])])
}

def get_face_info(img_p):
    """얼굴 임베딩과 시각화용 박스 좌표 추출"""
    try:
        img = Image.open(img_p).convert('RGB')
        boxes, _ = mtcnn.detect(img)
        if boxes is not None:
            face = mtcnn(img)
            if face is not None:
                emb = facenet(face.unsqueeze(0).to(device)).detach().cpu().numpy()
                return emb, boxes[0], True
    except: pass
    return None, None, False

def move_and_visualize_with_log(src_path, dest_path, box, log_item):
    """얼굴 박스 시각화, 파일 이동 및 상세 비교 로그 기록"""
    os.makedirs(QUARANTINE_FOLDER, exist_ok=True)
    img = cv2.imread(str(src_path))
    
    if img is not None and box is not None:
        # 얼굴에 초록색 박스 그리기 (FaceNet 좌표 활용)
        x1, y1, x2, y2 = map(int, box)
        cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 3)
        cv2.putText(img, "AI DUPLICATE", (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
    
    cv2.imwrite(str(dest_path), img)
    if os.path.exists(src_path): os.remove(src_path)
    
    # ⭐ [핵심 추가] 비교 원본 정보가 포함된 로그 저장
    with open(QUARANTINE_JSON_LOG, "a", encoding="utf-8") as f:
        f.write(json.dumps(log_item, ensure_ascii=False) + "\n")

if __name__ == "__main__":
    print(f"🚀 [RTX 4060] 비교 데이터 기록 모드 정제 시작")
    all_paths = [str(p) for p in Path(TARGET_DATE_FOLDER).glob("**/thumbnails/*.jpg")]
    if len(all_paths) < 2: exit()

    valid_paths, res_feats, face_data = [], [], {}
    for p in tqdm(all_paths, desc="🧠 특징 추출 및 분석"):
        try:
            img = Image.open(p).convert('RGB')
            # 1. ResNet 3-Pass 추출
            with torch.no_grad():
                f_avg = (resnet50(preprocess["center"](img).unsqueeze(0).to(device)) +
                         resnet50(preprocess["full"](img).unsqueeze(0).to(device)) +
                         resnet50(preprocess["zoom"](img).unsqueeze(0).to(device))) / 3.0
                res_feats.append(torch.flatten(f_avg, 1).cpu().numpy())
            
            # 2. 얼굴 정보(임베딩 + 좌표)
            emb, box, ok = get_face_info(p)
            if ok: face_data[p] = {"emb": emb, "box": box}
            valid_paths.append(p)
        except: continue

    res_feats = np.vstack(res_feats)
    res_sim_matrix = cosine_similarity(res_feats)

    deleted_indices = set()
    quarantine_tasks = []
    deleted_info_by_cat = {}

    for i in range(len(valid_paths)):
        if i in deleted_indices: continue
        for j in range(i + 1, len(valid_paths)):
            if j in deleted_indices: continue
            
            sim_res = res_sim_matrix[i][j]
            pi, pj = valid_paths[i], valid_paths[j]
            
            # 가중치 판별 (FaceNet 반영)
            if pi in face_data and pj in face_data:
                sim_face = cosine_similarity(face_data[pi]["emb"], face_data[pj]["emb"])[0][0]
                final_score = (sim_res * W_COSINE) + (sim_face * W_FACE)
            else:
                final_score = sim_res

            if final_score >= SIMILARITY_THRESHOLD:
                deleted_indices.add(j)
                p_pi, p_pj = Path(pi), Path(pj)
                cat = p_pj.parent.parent.name
                
                # ⭐ [핵심 추가] 비교 대상 정보를 로그 아이템으로 구성
                log_item = {
                    "duplicate_id": p_pj.stem,
                    "duplicate_path": pj,
                    "original_id": p_pi.stem,
                    "original_path": pi,
                    "score": round(float(final_score), 4),
                    "category": cat,
                    "processed_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                }
                
                quarantine_tasks.append({
                    "src": pj,
                    "box": face_data.get(pj, {}).get("box"),
                    "log_item": log_item
                })

                if cat not in deleted_info_by_cat: deleted_info_by_cat[cat] = []
                deleted_info_by_cat[cat].append(p_pj.stem)

    print(f"📦 {len(deleted_indices)}개 중복 격리 중 (비교 로그 생성)")
    for task in quarantine_tasks:
        dest = Path(QUARANTINE_FOLDER) / f"DUP_{Path(task['src']).name}"
        move_and_visualize_with_log(task['src'], dest, task['box'], task['log_item'])

    # JSON 업데이트 및 제목 없음 보정 (기존 기능 유지)
    for cat, d_ids in deleted_info_by_cat.items():
        j_path = Path(TARGET_DATE_FOLDER) / cat / f"{cat}_data.json"
        if j_path.exists():
            with open(j_path, "r", encoding="utf-8") as f:
                data_list = json.load(f)
            new_list = [item for item in data_list if item.get("id") not in d_ids]
            # 제목 보정
            for item in new_list:
                if not item.get('title') or item['title'].strip() == "":
                    item['title'] = "제목 없음"
            with open(j_path, "w", encoding="utf-8") as f:
                json.dump(new_list, f, ensure_ascii=False, indent=2)

    print("🎉 비교 정보가 포함된 정제 작업 완료.")