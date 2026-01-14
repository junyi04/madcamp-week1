# 📱 TikTok Meme Top 10 AI Pipeline

AI 기반 숏폼 콘텐츠 큐레이션 & 배포 시스템
Automated TikTok Content Curation with AI Filtering & REST API

본 프로젝트는 TikTok의 방대한 데이터 중 조회수가 검증되고(100k+), 한국어로 소통하며, 중복되지 않은 고품질 콘텐츠만 자동으로 선별하여 모바일 앱에 제공하는 End-to-End 자동화 파이프라인입니다.
KAIST 몰입캠프 Week 1 프로젝트

## 📑 목차

#### 프로젝트 개요
- 시스템 아키텍처
- 핵심 알고리즘
- 기술 스택
- 프로젝트 구조
- 설치 및 실행
- 보안 파일 설정
- API 문서
- 데이터베이스
- 배포 (ngrok)
- Android 연동
- 트러블슈팅

## 프로젝트 개요

#### 핵심 가치
특징|설명
:--|:-:
🤖 완전 자동화|매일 00:00 자동 크롤링 및 데이터 갱신
🧠 AI 필터링|ResNet50 + FaceNet 기반 중복 제거
🌏 언어 검증|Google Cloud AI를 통한 한국어 콘텐츠 선별
📊 품질 보증|100,000+ 조회수 검증된 콘텐츠만 수집
🚀 REST API|FastAPI 기반 고성능 API 서버
📱 모바일 연동|Android 앱 실시간 데이터 제공
🗂️ 히스토리 관리|3일간 데이터 보관 (파일 & DB 동기화)

## 🛠 시스템 아키텍처
#### 전체 파이프라인 (5-Step + Database + API)
~~~
┌─────────────────────────────────────────────────────────┐
│                   TikTok Platform                       │
└────────────────────┬────────────────────────────────────┘
                     │ 100k+ views filter
                     ↓
          ┌──────────────────────────┐
          │  Step 1: Crawling        │
          │  (Playwright Network     │
          │   Interception)          │
          └────────────┬─────────────┘
                       │ Raw JSON + Images
                       ↓
          ┌──────────────────────────┐
          │  Step 2: AI Deduplication│
          │  (ResNet50 + FaceNet)    │
          │  Similarity: 0.82        │
          └────────────┬─────────────┘
                       │ Unique Images
                       │ → MySQL: filtered_duplicates
                       ↓
          ┌──────────────────────────┐
          │  Step 3: Language Filter │
          │  (Google Translation API)│
          │  Korean Only             │
          └────────────┬─────────────┘
                       │ Korean Content
                       │ → MySQL: filtered_non_korean
                       ↓
          ┌──────────────────────────┐
          │  Step 4: Top10 Ranking   │
          │  (Views Sorting)         │
          │  URL Uniqueness          │
          └────────────┬─────────────┘
                       │ Top10 + Candidates
                       │ → MySQL: tiktok_videos
                       │ → MySQL: candidate_videos
                       ↓
          ┌──────────────────────────┐
          │  Step 5: Lifecycle Mgmt  │
          │  - 3-Day Retention       │
          │  - Auto Archive          │
          └────────────┬─────────────┘
                       │
                       ↓
          ┌──────────────────────────┐
          │    MySQL Database        │
          │  (날짜별 3일간 보관)      │
          └────────────┬─────────────┘
                       ↓
          ┌──────────────────────────┐
          │   FastAPI REST API       │
          │   (Uvicorn Server)       │
          └────────────┬─────────────┘
                       ↓
          ┌──────────────────────────┐
          │     ngrok Tunnel         │
          │  (HTTPS + Fixed Domain)  │
          └────────────┬─────────────┘
                       ↓
          ┌──────────────────────────┐
          │    Android Mobile App    │
          │  (Kotlin + Compose)      │
          └──────────────────────────┘
~~~

## Step-by-Step 상세 설명
#### Step 1: Intelligent Crawling (crawling.py)
기술: Playwright Network Interception
#### 프로세스:
1. Chrome 브라우저 자동화 (User Session 유지)
2. TikTok API 응답 가로채기 (Network Interception)
3. 마우스 휠 시뮬레이션으로 봇 탐지 우회
4. 100,000+ 조회수 필터 (1차 품질 검증)
5. 중복 방지 (seen_video_ids 세션 관리)
6. JSONL 형식 저장
#### 대상 카테고리:
- main - 전체 통합
- dance - 댄스 챌린지
- challenge - 바이럴 챌린지
- food - 먹방/요리
- tts - 텍스트 음성 변환
#### 출력:
~~~
2026-01-14/
├─ dance/
│  ├─ dance_data.json      # 100+ videos
│  └─ thumbnails/
│     ├─ image001.jpg
│     ├─ image002.jpg
│     ...
~~~
#### 🧠 Step 2: 3-Pass AI Feature Extraction (cnn_pic_dec.py)
기술: ResNet50 + FaceNet + TTA (Test Time Augmentation)
#### 알고리즘:
2-1. 3-Pass TTA (정확도 극대화)
~~~
# 한 이미지를 3가지 시점으로 분석
transforms = [
    CenterCrop(224),    # 중앙 집중
    Resize(256),        # 전체 뷰
    RandomCrop(224)     # 확대 뷰
]
# → 평균 특징 벡터 사용
```

##### 2-2. 코사인 유사도 계산
```
similarity(A, B) = (A · B) / (||A|| × ||B||)

threshold = 0.82
```

**판정:**
- **1.0**: 완전히 동일
- **0.82 이상**: 중복 판정 (미세 변형 포함)
- **0.82 미만**: 다른 이미지

**출력:**
```
duplicates_storage/
├─ DUP_image005.jpg    # 초록색 바운딩 박스 표시
├─ DUP_image012.jpg
...

MySQL: filtered_duplicates (날짜별 3일 보관)
~~~
#### 🌏 Step 3: Google AI NLP Filtering (google_language_detector.py)
기술: Google Cloud Translation API v2
#### 로직: 이중 검증 (Double Validation)
~~~
# 1차: 제목 검사
if is_korean(title):
    return True

# 2차: 제목 없을 때 작성자명 검사
if not title and is_korean(author):
    return True  # 유실 방지

# 제외
return False
```

**출력:**
```
non_korean_quarantine/
├─ nonko_dance_image003.jpg
├─ nonko_challenge_image007.jpg
...

MySQL: filtered_non_korean (날짜별 3일 보관)
~~~
#### 🏆 Step 4: Ultimate Ranking & ID Remapping (top10_filter.py)
기술: URL Uniqueness + View Count Sorting
#### 프로세스:
4-1. Main 카테고리 통합
~~~
# 모든 카테고리 → main 폴더로 복사
main_data = dance + challenge + food + tts
~~~
4-2. URL 기반 중복 제거
~~~
unique_map = {}
for video in all_videos:
    url = video['url']
    # 같은 URL = 같은 영상
    if url not in unique_map:
        unique_map[url] = video
    else:
        # 조회수 높은 것만 유지
        if video['views'] > unique_map[url]['views']:
            unique_map[url] = video
~~~
4-3. Top10 선정 및 ID 재부여
~~~
# 조회수 내림차순 정렬
sorted_videos = sorted(unique_map.values(), 
                       key=lambda x: x['views'], 
                       reverse=True)[:10]

# ID 재부여: dance01, dance02, ...
for i, video in enumerate(sorted_videos, 1):
    video['id'] = f"{category}{str(i).zfill(2)}"
~~~
출력:
~~~
{
  "id": "dance01",
  "title": "춤 제목",
  "author": "작성자",
  "views": 5000000,
  "likes": 250000,
  "category": "dance",
  "url": "https://tiktok.com/@user/video/123...",
  "image_file": "2026-01-14/dance/top10/thumbnails/dance01.jpg"
}
```

**MySQL 저장:**
```
- tiktok_videos: Top10 (50개)
- candidate_videos: 11위 이하
- 날짜별 3일 보관
```

---

#### 💾 **Step 5: Storage Lifecycle Management** (`top10_filter.py`)

**파일 시스템:**
```
2026-01-14/
└─ dance/
   └─ top10/                    # 최종 배포용
      ├─ dance_top10.json
      └─ thumbnails/
         ├─ dance01.jpg
         ├─ dance02.jpg
         ...

residual_archive/
└─ 2026-01-14/
   └─ dance/
      ├─ dance_raw.json         # 원본 데이터
      └─ thumbnails/            # 원본 이미지
~~~
MySQL 데이터:
~~~
-- 날짜별 3일간 보관
SELECT DATE(created_at), COUNT(*) 
FROM tiktok_videos 
GROUP BY DATE(created_at);

2026-01-14 | 50개  ← 오늘
2026-01-13 | 50개  ← 어제
2026-01-12 | 50개  ← 그제
2026-01-11 | 자동 삭제됨 (3일 경과)
~~~
자동 삭제 (Retention: 3 Days):
~~~
# 파일 & DB 동시 관리
if (today - folder_date).days >= 3:
    shutil.rmtree(folder)              # 파일 삭제
    DELETE FROM ... WHERE DATE < ...   # DB 삭제
~~~
## 🔬 핵심 알고리즘
1. Playwright 네트워크 가로채기\
~~~
async def intercept_api_response(route, request):
    response = await route.fetch()
    if "/api/recommend/item_list" in request.url:
        json_data = await response.json()
        # 실제 TikTok API 응답 파싱
        extract_video_data(json_data)
~~~
장점:
- ✅ DOM 파싱 불필요
- ✅ 정확한 데이터
- ✅ 봇 탐지 회피
2. 코사인 유사도
~~~
def cosine_similarity(A, B):
    """
    A, B: 2048차원 특징 벡터 (ResNet50)
    """
    dot_product = np.dot(A, B)
    norm_A = np.linalg.norm(A)
    norm_B = np.linalg.norm(B)
    
    return dot_product / (norm_A * norm_B)
~~~
3. URL 중복 방지
~~~
unique_map = {}
for video in all_videos:
    url = video['url']
    
    if url not in unique_map:
        unique_map[url] = video
    else:
        # 조회수 많은 버전만 유지
        if video['views'] > unique_map[url]['views']:
            unique_map[url] = video
```

---

## 💻 기술 스택

### Backend

| 기술 | 버전 | 용도 |
|------|------|------|
| Python | 3.8+ | 메인 언어 |
| FastAPI | 0.104+ | REST API |
| Uvicorn | 0.24+ | ASGI 서버 |
| APScheduler | 3.10+ | 스케줄링 |
| MySQL | 8.0+ | 데이터베이스 |

### AI/ML

| 라이브러리 | 용도 |
|-----------|------|
| PyTorch | 딥러닝 프레임워크 |
| torchvision | ResNet50 |
| facenet-pytorch | FaceNet |
| scikit-learn | 유사도 계산 |

### Crawling

| 라이브러리 | 용도 |
|-----------|------|
| Playwright | 브라우저 자동화 |
| google-cloud-translate | 언어 감지 |

### Deployment

| 기술 | 용도 |
|------|------|
| ngrok | HTTPS 터널링 |

### Mobile

| 기술 | 용도 |
|------|------|
| Kotlin | Android |
| Jetpack Compose | UI |
| Retrofit | HTTP 클라이언트 |
| Coil | 이미지 로딩 |

---

## 📂 프로젝트 구조
```
Project_Root/
│
├─ 📄 Python Pipeline
│  ├─ crawling.py                    # Step 1
│  ├─ cnn_pic_dec.py                 # Step 2
│  ├─ google_language_detector.py    # Step 3
│  ├─ top10_filter.py                # Step 4-5
│  ├─ main.py                        # FastAPI 서버
│  └─ category.py                    # 카테고리 API
│
├─ 🔐 보안 파일 (Git 제외)
│  ├─ google_key.json                # Google API Key
│  ├─ ngrok.yml                      # ngrok authtoken
│  └─ .env
│
├─ 📊 상태 & 로그
│  ├─ crawl_status.json              # 크롤링 상태
│  └─ execution_log.txt              # 실행 로그
│
├─ 🤖 AI Models
│  └─ models/
│     └─ resnet50-19c8e357.pth
│
├─ 🗄️ Database
│  └─ db_schema.sql
│
├─ 🗃️ 데이터 (날짜별 3일 보관)
│  ├─ 2026-01-14/                    # 오늘
│  │  ├─ main/
│  │  │  └─ top10/
│  │  │     ├─ main_top10.json
│  │  │     └─ thumbnails/
│  │  ├─ dance/
│  │  ├─ challenge/
│  │  ├─ food/
│  │  └─ tts/
│  │
│  ├─ 2026-01-13/                    # 어제
│  ├─ 2026-01-12/                    # 그제
│  │
│  ├─ residual_archive/              # 원본 보관
│  │  ├─ 2026-01-14/
│  │  ├─ 2026-01-13/
│  │  └─ 2026-01-12/
│  │
│  ├─ duplicates_storage/            # 중복 격리
│  └─ non_korean_quarantine/         # 비한국어 격리
│
├─ 📚 문서
│  ├─ README.md
│  └─ requirements.txt
│
└─ 🔧 설정
   ├─ .gitignore
   └─ .venv/
~~~
#
#### 🚀 설치 및 실행
사전 요구사항
- ✅ Python 3.8+
- ✅ MySQL 8.0+
- ✅ Google Cloud 계정
- ✅ ngrok 계정 (유료 권장)
- ✅ Chrome 브라우저
#
1️⃣ 저장소 클론
~~~
git clone https://github.com/yourusername/tiktok-meme-pipeline.git
cd tiktok-meme-pipeline
~~~
2️⃣ 가상환경 설정
~~~
# 생성
python -m venv .venv

# 활성화 (Windows)
.venv\Scripts\activate

# 활성화 (Linux/Mac)
source .venv/bin/activate
~~~
3️⃣ 패키지 설치
~~~
pip install -r requirements.txt
playwright install chromium
~~~
requirements.txt:
~~~
fastapi==0.104.1
uvicorn[standard]==0.24.0
mysql-connector-python==9.0.0
apscheduler==3.10.4
python-multipart==0.0.6
playwright==1.40.0
torch==2.1.0
torchvision==0.16.0
facenet-pytorch==2.5.3
google-cloud-translate==3.12.1
Pillow==10.1.0
tqdm==4.66.1
scikit-learn==1.3.2
numpy==1.24.3
~~~
4️⃣ MySQL 설정
~~~
# MySQL 시작
net start MySQL80  # Windows
sudo systemctl start mysql  # Linux
~~~
sql
~~~
-- 데이터베이스 생성
CREATE DATABASE madcamp1_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE madcamp1_db;

-- 테이블 생성
SOURCE db_schema.sql;
~~~
#
#### 🔐 보안 파일 설정
1️⃣ google_key.json 생성
- Google Cloud Console:
1. https://console.cloud.google.com 접속
2. 프로젝트 생성
3. API 및 서비스 → 라이브러리
4. "Cloud Translation API" 검색 → 사용 설정
5. 사용자 인증 정보 → API 키 생성
6. 키 복사
- 프로젝트 루트에 생성:
~~~
{
  "api_key": "AIzaSyD...실제키...abc123"
}
~~~
2️⃣ ngrok.yml 생성
- ngrok Dashboard:
1. https://dashboard.ngrok.com 로그인
2. Your Authtoken 메뉴
3. 토큰 복사
- 프로젝트 루트에 생성:
~~~
version: "2"
authtoken: 2q...실제토큰...xyz
update_channel: stable
region: jp
~~~
- Reserved Domain (유료 플랜 $10/월):
1. Dashboard → Domains
2. + New Domain
3. 도메인 이름 입력
4. Region: Japan
5. Create
#
3️⃣ .gitignore 생성
- 프로젝트 루트에 생성:
~~~
# 보안 파일
google_key.json
ngrok.yml
.env

# Python
.venv/
__pycache__/
*.pyc
*.pyo
*.pyd

# 데이터
2026-*/
residual_archive/
duplicates_storage/
non_korean_quarantine/
crawl_status.json
execution_log.txt

# AI Models
models/

# IDE
.vscode/
.idea/
*.swp
~~~
4️⃣ 설정 파일 수정
- 모든 Python 파일에서:
~~~
# main.py, top10_filter.py, category.py, 
# cnn_pic_dec.py, google_language_detector.py

SERVER_DOMAIN = "your-domain.ngrok.app"  # 변경!

MYSQL_CONFIG = {
    'host': 'localhost',
    'database': 'madcamp1_db',
    'user': 'root',
    'password': 'your_password'  # 변경!
}
~~~
5️⃣ 실행
- 터미널 1 - ngrok:
~~~
ngrok http 8001 --config ngrok.yml --domain your-domain.ngrok.app
~~~
- 터미널 2 - 서버:
~~~
python main.py
```

**테스트:**
```
https://your-domain.ngrok.app/
https://your-domain.ngrok.app/top10
https://your-domain.ngrok.app/docs
```

---

## 📡 API 문서

### Base URL
```
https://your-domain.ngrok.app
~~~
#### 주요 엔드포인트

1. 상태 확인
~~~
GET /
~~~
2. Main Top10
~~~
GET /top10
~~~
Response:
~~~
[
  {
    "id": "main01",
    "title": "춤추는 고양이",
    "author": "작성자123",
    "views": 5000000,
    "likes": 250000,
    "category": "main",
    "url": "https://www.tiktok.com/@user/video/...",
    "imageFile": "https://your-domain.ngrok.app/2026-01-14/main/top10/thumbnails/main01.jpg"
  }
]
~~~
3. 카테고리별
~~~
GET /api/category/{name}
~~~
- name: dance, challenge, food, tts
4. 전체 비디오
~~~
GET /api/all-videos
~~~
5. 필터링 데이터
~~~
GET /api/filtered/non-korean
GET /api/filtered/duplicates
GET /api/candidates
```

**Swagger UI:**
```
https://your-domain.ngrok.app/docs
```

---

## 🗄️ 데이터베이스

### 날짜별 데이터 관리 (3일 보관)

**파일 시스템과 동기화:**
```
파일: 2026-01-14/, 2026-01-13/, 2026-01-12/
DB:   2026-01-14,  2026-01-13,  2026-01-12
      (created_at 기준)
~~~
#
테이블 구조
- tiktok_videos (Top10)
~~~
CREATE TABLE tiktok_videos (
    id VARCHAR(50) PRIMARY KEY,
    title TEXT,
    author VARCHAR(255),
    views BIGINT,
    likes BIGINT,
    category VARCHAR(50),
    url TEXT,
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_views (views)
);
~~~
- candidate_videos (후보군)
~~~
CREATE TABLE candidate_videos (
    id VARCHAR(50) PRIMARY KEY,
    rank_in_category INT,
    filtered_date VARCHAR(20),
    main_video_id VARCHAR(50),
    FOREIGN KEY (main_video_id) 
        REFERENCES tiktok_videos(id) 
        ON DELETE CASCADE
);
~~~
- filtered_non_korean (비한국어)
~~~
CREATE TABLE filtered_non_korean (
    id VARCHAR(50) PRIMARY KEY,
    detected_language VARCHAR(10),
    filtered_date VARCHAR(20),
    ...
);
~~~
- filtered_duplicates (중복)
~~~
CREATE TABLE filtered_duplicates (
    id VARCHAR(50) PRIMARY KEY,
    similarity_score DECIMAL(5,4),
    filtered_date VARCHAR(20),
    ...
);
~~~
- 날짜별 조회
~~~
-- 오늘 데이터
SELECT * FROM tiktok_videos 
WHERE DATE(created_at) = '2026-01-14';

-- 날짜별 개수
SELECT DATE(created_at) as date, COUNT(*) 
FROM tiktok_videos 
GROUP BY DATE(created_at)
ORDER BY date DESC;
~~~
#
### 🌐 배포 (ngrok)
ngrok이란?

로컬 서버를 HTTPS로 외부에 공개하는 터널링 서비스

무료 vs 유료
항목|무료|유료 ($10/월)
:-:|:-:|:-:
URL|랜덤 변경|고정 도메인 ✅
경고|"Are you developer?|"없음 ✅
안정성|불안정|안정적 ✅

실행
~~~
# Reserved Domain
ngrok http 8001 --config ngrok.yml --domain your-domain.ngrok.app

# Region 변경
ngrok http 8001 --region us
~~~
#
### 📱 Android 연동
Gradle
~~~
dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
}
~~~
API 인터페이스
~~~
interface TikTokApi {
    @GET("top10")
    suspend fun getTop10(): List<VideoData>
    
    @GET("api/category/{name}")
    suspend fun getCategory(@Path("name") category: String): List<VideoData>
}
~~~
Retrofit 클라이언트
~~~
object RetrofitClient {
    private const val BASE_URL = "https://your-domain.ngrok.app/"
    
    val api: TikTokApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TikTokApi::class.java)
    }
}
~~~
#
### 🐛 트러블슈팅
1. MySQL 연결 실패
~~~
net start MySQL80
mysql -u root -p
~~~
2. ngrok 연결 실패
~~~
ngrok config add-authtoken YOUR_TOKEN
ngrok http 8001 --region us
~~~
3. Foreign Key 에러
~~~
SET FOREIGN_KEY_CHECKS = 0;
-- 작업 수행
SET FOREIGN_KEY_CHECKS = 1;
~~~
4. 중복 키 에러
~~~
-- ON DUPLICATE KEY UPDATE 사용
INSERT INTO ... VALUES (...) 
ON DUPLICATE KEY UPDATE views=VALUES(views);
~~~
5. Google API 할당량 초과

- API 키 확인
- 할당량 증가 신청
#
### 📄 라이센스
MIT License
#
#### 🙏 감사의 말

- KAIST 몰입캠프
- 전남대학교 컴퓨터공학과
- Google Cloud Platform
- ngrok
- PyTorch Community
#
#### 📞 문의
- Email: ggeonhui78@gmail.com
#
#### 🔗 관련 링크
- FastAPI Docs
- Playwright Docs
- ngrok Docs
- PyTorch Docs
#
Last Updated: 2026-01-14

Version: 2.1.0
#
#### 개발자
> 강준이, 김건희

⭐ Star this project if you find it helpful!

