# 📱 TikTok Meme Top 10 AI Pipeline
AI 기반 숏폼 콘텐츠 큐레이션 시스템 > 본 프로젝트는 틱톡의 방대한 데이터 중 조회수가 검증되고(100k+), 한국어로 소통하며, 중복되지 않은 고품질 릴스만 자동으로 선별하여 사용자에게 제공하는 자동화 파이프라인입니다.
## 🛠 1. 시스템 아키텍처 (System Architecture)
본 시스템은 데이터 수집부터 최종 배포 데이터 생성까지 총 5단계의 정제 과정을 거칩니다.
### Step 1: Intelligent Crawling (Playwright)
- 대상: 5개 핵심 카테고리 (Main, Dance, Challenge, Food, TTS)
- 필터: 10만(100,000) 이상의 조회수를 기록한 영상만 1차 수집
- 보안: Chrome 사용자 세션 유지 및 마우스 휠 시뮬레이션을 통해 봇 탐지 우회
- 매일 00시에 크롤링되며 crawl_status.json에 금일 크롤링 여부가 적히며 failed 일시 12시에 크롤링을 진행한다(2026-01-11에 수정)
### Step 2: 3-Pass AI Feature Extraction (PyTorch)
단순한 이미지 비교가 아닌, 딥러닝 모델 ResNet50을 특징 추출기로 사용하여 이미지의 '지문'을 생성합니다.
- TTA(Test Time Augmentation): 한 장의 이미지를 세 가지 시점(Center Crop, Full View, Zoom View)으로 분석하여 정확도를 극대화합니다
- Cosine Similarity: 추출된 특징 벡터 간의 각도를 계산하여 시각적 유사성을 판별합니다
### Step 3: Google AI NLP Filtering (NLP)
- Google Cloud Translation API: 영상 제목의 문맥을 분석하여 언어를 감지합니다
- 이중 검증 로직: 제목이 없는 경우 작성자(Author)의 닉네임을 2차로 분석하여 유용한 한국어 콘텐츠의 유실을 방지합니다

### Step 4: Ultimate Ranking & ID Remapping
- URL Uniqueness: 영상의 고유 URL을 Key로 사용하여 동일 영상이 순위권에 중복 노출되는 현상을 원천 봉쇄합니다
- ID Mapping: [Category][Number] 형식(예: dance01)으로 ID를 재부여하여 앱 서비스 활용성을 높입니다

### Step 5: Storage Lifecycle Management
- TOP 10 Isolation: 최종 선정된 데이터는 top10/ 폴더로 격리 보관
- Residual Archive: 나머지 데이터는 보관소로 이동 후 3일(Retention: 3 Days) 뒤 자동 영구 삭제
- 파일 실행시 crawl_status.json 파일 생성 및 failed -> success 으로 수정(2026-01-11에 수정)

## 📂 프로젝트 폴더 구조 (Directory Structure)
Project_Root/   
| crawling.py   # 1단계: 수집 및 기본 중복 방지  
| cnn_pic_dec.py   # 2단계: 불법 복사된 중복 릴스 제거   
| google_language_detector.py   # 3단계: Google AI NLP Filtering(한국 릴스만 추출)   
| top10_filter.py   # 4,5단계: 고유 URL 기반 중복 영상 랭킹 방지 및 id 랭킹에 맞게 변환 및 저장
| run_pipeline.py   # 서버에서 1~5 단계 파일 자동 실행
| api_server.py   # 서버 실행 파일(여거 실행하면 나머지 자동화)
| google_key.json   # Google Translation API key   
| crawl_status.json   # 금일 크롤링 여부 체크 파일
| execution_log.txt   # 서버 크롤링 실행 시간 로그 저장
| models/   #ResNet50 가중치   
| residual_archive/   # 3일 뒤 삭제될 임시 데이터 보관소   
|  └─ 2026-01-06/   # (3일 경과 시 자동 삭제되는 날짜 폴더)   
|   
|─ 2026-01-09/            # 당일 작업 결과물   
  └─ dance/              # 카테고리 폴더   
      └─ top10/           # 최종 정제된 핵심 데이터 (안드로이드 배포용)   
      ├─ thumbnails/   # dance01.jpg ~ dance10.jpg   
         └─ dance_top10.json
## 🔬 핵심 알고리즘 상세 설명
이미지 유사도 계산 (Cosine Similarity)두 이미지의 특징 벡터 $\mathbf{A}$와 $\mathbf{B}$ 사이의 코사인 유사도는 다음과 같이 계산됩니다
- 결과값이 1.0에 가까울수록 시각적으로 동일한 이미지로 판단합니다
- 본 프로젝트는 0.82를 임계값(Threshold)으로 설정하여 미세한 변형이 가해진 재업로드 영상을 식별합니다
- 
## 📝 설치 및 실행 (Setup)
1. 가상환경 설정
~~~ 
python -m venv .venv
.venv\Scripts\activate
~~~
2. 필수 라이브러리 설치
~~~
pip install torch torchvision google-cloud-translate imagehash pillow tqdm scikit-learn
python -m pip install apscheduler
python -m pip install fastapi uvicorn 
~~~
3. 실행 순서
- google_key.json 파일이 루트 폴더에 있는지 확인합니다.
- 터미널에서 실행
~~~
 python api_server.py
~~~
### 수정사항
1. top10_filter.py 파일 json 양식 안드로이드 스튜디오 양식에 맞춰지게 코드 수정(2026-01-10)
2. crawling.py 파일 수정 서버에서 파일 생성시 윈도우가 잠기는 문제 해결 및 execution_log.txt 파일 생성 및 수정되게 수정(2026-01-11)
3. api_server.py, run_pipeline.py 파일 생성(2026-01-11)
4. top10_filter.py 파일 crawl_status.json 생성 및 수정되게 수정(2026-01-11)
5. api_server.py 서버가 켜질 때 오늘 크롤링 성공 여부를 확인해 자동으로 작업되게 수정(2026-01-11)




