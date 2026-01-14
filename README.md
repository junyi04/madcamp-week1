# 밈포티

> 난 아직 젊다! 젊은 세대와 함께 걸어가는 최신 유행 밈 큐레이션 앱

## 📱 소개

**영포티 밈 저장소**는 20대와 함께 걸어가고 싶은, 혹은 아직도 20대인 것 같이 살고 싶은 사람들을 위한 최신 유행 밈 큐레이션 앱입니다. 틱톡 크롤링을 기반으로 매일 자정 Top 10 밈 영상을 업데이트하여, 항상 최신 트렌드를 놓치지 않도록 도와줍니다.

### ✨ 주요 특징

- **매일 Top 10 업데이트**: 틱톡 크롤링 기반으로 매일 자정 가장 핫한 밈 Top 10 자동 업데이트
- **카테고리별 분류**: 춤, 챌린지, 음식, TTS 다양한 카테고리로 구성된 밈 컬렉션
- **출석체크 시스템**: 매일 방문 보상 및 연속 출석 달력 제공
- **알림 기능**: 새로운 밈 업데이트 시 푸시 알림으로 실시간 알림
- **레트로 UI**: 사이버펑크 감성의 독특한 디자인


---
## 🚀 시작하기

### 최초 실행 플로우

앱을 처음 실행하면 다음 순서로 진행됩니다:

1. **온보딩 화면** (최초 1회)
    - 앱의 주요 기능 소개
    - 3단계 스텝으로 구성된 인터랙티브 가이드

2. **알람 설정**
    - 매일 자정 Top 10 업데이트 알림 설정

3. **출석체크 모달**
    - 첫 방문 보상 및 출석 시스템 안내

---

## 📋 주요 기능

### 1. 홈 화면 (Top 10)

![Home Screen](https://via.placeholder.com/800x400?text=Top+10+Screen)

- **매일 자정 업데이트**: 틱톡에서 가장 인기 있는 밈 Top 10을 매일 자동 업데이트
- **영상 정보 표시**:
    - 썸네일 이미지
    - 순위 표시
    - 제목 및 조회수
    - 카테고리 태그
- **인터랙션**:
    - **보기 버튼**: 틱톡 앱으로 바로 이동하여 영상 시청
    - **좋아요 버튼**: 관심 있는 밈 저장

### 2. 카테고리 화면

![Categories Screen](https://via.placeholder.com/800x400?text=Categories+Screen)

4가지 주요 카테고리로 구성:
- **춤**: 유행하는 댄스 챌린지
- **챌린지**: 바이럴 챌린지 모음
- **음식**: 먹방 및 음식 관련 밈
- **TTS**: Text-to-Speech 기반 밈

각 카테고리별로 정리된 영상 리스트를 탭으로 전환하며 탐색할 수 있습니다.

### 3. 출석체크 화면

![Attendance Screen](https://via.placeholder.com/800x400?text=Attendance+Screen)

- **달력 UI**: 월간 출석 현황 한눈에 확인
- **출석 통계**:
    - 총 출석일 수
    - 연속 출석 일수
    - 출석 보상 점수
- **자동 출석**: 앱 실행 시 자동으로 오늘 출석 체크
- **보상 시스템**: 연속 출석 시 특별 보상 제공

### 4. 3컷 만화 화면

- 순수 재미를 위한 3컷 만화
- **영포티** 아자스! 

---

## 🛠️ 기술 스택
### Client
---
#### Android

- **Language**: Kotlin 2.0.21
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

#### UI Framework

- **Jetpack Compose**: 모던한 UI 구현 (출석체크, 온보딩)
- **Material Design 3**: 일관된 디자인 시스템
- **XML Layouts**: 기존 뷰 시스템 활용

#### 네트워킹
- **Retrofit2**: FAST API 서버 통신 
- **Gson Converter**: JSON 데이터 파싱

#### 이미지 로딩
- **Glide 4.16.0**: 효율적인 이미지 캐싱 및 로딩

#### UI Components
- **RecyclerView**: 리스트 렌더링
- **ConstraintLayout**: 복잡한 레이아웃 구성
- **Material Components**: Material Design 3 컴포넌트

#### Jetpack Compose
- **Compose BOM 2024.02.00**
- **Material3**: Compose용 Material Design 3
- **Material Icons Extended**: 확장 아이콘 세트
---
### Backend
---
#### Server
- **Language**: Python 3.8+
- **Architecture**: REST API Architecture

#### Framework & Server
- **FastAPI**: 고성능 비동기 REST API 서버 구축
- **Uvicorn**: ASGI 웹 서버 (FastAPI 구동)
- **APScheduler**: 매일 자정 크롤링 파이프라인 자동화

#### Database & Infra
- **MySQL 8.0**: 메타데이터 영구 저장 및 3일 주기 데이터 관리
- **ngrok**: HTTPS 터널링을 통한 외부 접속 환경 제공

#### AI & Machine Learning
- **PyTorch & torchvision**: 딥러닝 모델(ResNet50) 구동 및 추론
- **FaceNet**: 얼굴 특징 추출 및 유사도 분석
- **Google Cloud Translation API**: 한국어 콘텐츠 이중 검증(NLP)

#### Crawling
- **Playwright**: 네트워크 패킷 인터셉트 기반 고품질 데이터 수집
---

## 🔧 설치 및 실행

### 사전 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17 이상
- Android SDK API 34
- Python 3.8 이상
- MySQL 8.0 이상
- Chrome Browser (Playwright용)

### 설치 방법

1. **저장소 클론**
```bash
git clone https://github.com/yourusername/madcamp_week1.git
cd madcamp_week1
```

2. **서버 연동 설정 (FastAPI & Ngrok)**
    - 필수: 백엔드(main.py) 실행 후 생성된 ngrok URL이 필요
    - ngrok에서 토큰을 받아와 ngrok.yml 넣고 저장
    - 서버 실행시
    > ngrok http 8001
3. **서버 URL 설정**

   `MainActivity.kt`, `CategoriesActivity.kt`에서 ngrok URL 설정:
   ```kotlin
   val ngrokUrl = "https://your-ngrok-url.ngrok-free.dev/"
   ```

4. **빌드 및 실행**
```bash
./gradlew assembleDebug
```

또는 Android Studio에서 `Run` 버튼 클릭

---

## 🔌 API 엔드포인트

### Base URL
```
https://your-server.ngrok-free.dev/
# (주의: ngrok 실행 시마다 변경되는 도메인을 사용해야 합니다)
```

### Endpoints

#### 1. Top 10 영상 조회
```http
GET /top10
```

**Response:**
```json
[
  {
    "id": "dance05",
    "title": "제목",
    "author": "작성자",
    "views": 1234567,
    "likes": 50000,
    "url": "https://tiktok.com/@user/video/123",
    "imageFile": "imageFile": "https://your-server.ngrok-free.dev/2026-01-14/main/top10/thumbnails/dance05.jpg",
    "category": "dance"
  }
]
```

#### 2. 카테고리별 영상 조회
```http
GET /api/category/{name}
```

**Parameters:**
- `name`: `dance` | `challenge` | `food` | `tts`

**Response:**
```json
[
  {
    "id": "dance05",
    "title": "챌린지 제목",
    "author": "작성자",
    "views": 987654,
    "likes": 30000,
    "url": "https://tiktok.com/@user/video/456",
    "imageFile": "https://your-server.ngrok-free.dev/2026-01-14/dance/top10/thumbnails/dance05.jpg",
    "category": "dance"
  }
]
```

---

## 📦 주요 의존성

```toml
[versions]
kotlin = "2.0.21"
agp = "8.9.1"
compose-bom = "2024.02.00"
retrofit = "2.9.0"
glide = "4.16.0"
firebase-bom = "33.7.0"

[libraries]
# Networking
retrofit = "com.squareup.retrofit2:retrofit:2.9.0"
retrofit-converter-gson = "com.squareup.retrofit2:converter-gson:2.9.0"

# Image Loading
glide = "com.github.bumptech.glide:glide:4.16.0"

# Firebase
firebase-bom = "com.google.firebase:firebase-bom:33.7.0"
firebase-analytics = "com.google.firebase:firebase-analytics"
firebase-messaging = "com.google.firebase:firebase-messaging"
firebase-firestore = "com.google.firebase:firebase-firestore-ktx"

# Compose
compose-bom = "androidx.compose:compose-bom:2024.02.00"
material3 = "androidx.compose.material3:material3"
```

---

## 🎯 주요 기능 구현

### 1. 온보딩 시스템

첫 실행 시 3단계 온보딩 제공:
- Step 1: 앱 소개 및 주요 기능
- Step 2: 카테고리 설명
- Step 3: 출석 체크 시스템 안내

```kotlin
OnboardingModal(
    isOpen = showOnboarding,
    onComplete = { 
        // 온보딩 완료 처리
    }
)
```

### 2. 자동 출석 체크

앱 실행 시 자동으로 오늘 출석 체크:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    attendanceManager = AttendanceManager(this)
    attendanceManager.checkTodayAttendance()
}
```

### 3. 알림 스케줄링

매일 자정 알림 설정:

```kotlin
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 알림 발송 로직
    }
}
```

### 4. 동적 카테고리 로딩

탭 선택 시 서버에서 카테고리 데이터 로드:

```kotlin
private fun fetchCategoryDataFromServer(categoryName: String, uiTitle: String) {
    apiService.getCategoryData(categoryName).enqueue(object : Callback<List<VideoData>> {
        override fun onResponse(call: Call<List<VideoData>>, response: Response<List<VideoData>>) {
            // 데이터 업데이트
        }
    })
}
```

### 5. 서버 이미지 렌더링 (Glide)

FastAPI 서버(Ngrok)에서 서빙하는 로컬 이미지를 효율적으로 로딩 및 캐싱:

~~~
Glide.with(context)
    .load(videoData.imageFile) // 서버의 ngrok URL (예: https://.../dance01.jpg)
    .diskCacheStrategy(DiskCacheStrategy.ALL) // 리소스 절약을 위한 캐싱
    .placeholder(R.drawable.placeholder)
    .into(binding.thumbnailImageView)
~~~

### 6. 틱톡 딥링크 연동 (Intent)

'보기' 버튼 클릭 시 틱톡 앱으로 바로 이동하거나 웹으로 연결:

~~~
val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoData.url))
try {
    // 틱톡 앱이 설치되어 있으면 앱으로 실행
    startActivity(intent)
} catch (e: ActivityNotFoundException) {
    // 없으면 브라우저로 틱톡 웹페이지 실행
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoData.url)))
}
~~~

### 7. [Backend] AI 중복 제거 파이프라인

ResNet50과 FaceNet을 활용하여 수집된 밈의 이미지 유사도를 분석, 중복 콘텐츠(유사도 0.82 이상)를 자동 필터링:

~~~
# cnn_pic_dec.py (Core Logic)
def check_duplicate(new_img_path, existing_imgs):
    vec1 = get_feature_vector(new_img_path) # ResNet50 특징 추출
    
    for img in existing_imgs:
        vec2 = get_feature_vector(img)
        similarity = cosine_similarity(vec1, vec2)
        
        if similarity > 0.82: # 임계값 초과 시 중복 판정
            return True, img
            
    return False, None
~~~

### 8. [Backend] 지능형 크롤링 (Playwright)

봇 탐지를 우회하고 네트워크 패킷을 직접 가로채어(Interception) 고품질 데이터 수집:

~~~
# crawling.py
async def intercept_response(route, request):
    if "/api/recommend/item_list" in request.url:
        data = await route.fetch()
        json_data = await data.json()
        
        # 조회수 10만 이상 & 한국어 영상만 필터링하여 저장
        process_video_data(json_data) 
        
    await route.continue_()
~~~

### 9. [Backend] 데이터 수명 주기 관리 (MySQL)

FastAPI 서버와 MySQL을 연동하여 데이터를 영구 저장하고, 최신 트렌드 유지를 위해 3일이 지난 데이터(파일 및 DB 로그)를 자동으로 소각하는 로직 구현:

~~~
# top10_filter.py (Data Retention Policy)
def cleanup_old_data(conn):
    cursor = conn.cursor()
    # 3일 전 날짜 계산
    limit_date = (datetime.now() - timedelta(days=3)).strftime('%Y-%m-%d')
    
    # 1. DB 메타데이터 삭제
    sql = "DELETE FROM tiktok_videos WHERE DATE(created_at) < %s"
    cursor.execute(sql, (limit_date,))
    
    # 2. 로컬 이미지 파일 삭제 (스토리지 최적화)
    if os.path.exists(old_folder_path):
        shutil.rmtree(old_folder_path)
        
    conn.commit()
~~~
---

## 👥 개발팀

- **프로젝트 기간**: KAIST 몰입캠프 Week 1
- **개발 환경**: Android Studio, Kotlin
