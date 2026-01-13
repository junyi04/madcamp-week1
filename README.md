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

### 4. 알림 화면

- 매일 자정 Top 10 업데이트 알림
- Firebase Cloud Messaging (FCM) 기반 푸시 알림
- 알림 설정 관리

---

## 🛠️ 기술 스택

### Android

- **Language**: Kotlin 2.0.21
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34 (Android 14)

### UI Framework

- **Jetpack Compose**: 모던한 UI 구현 (출석체크, 온보딩)
- **Material Design 3**: 일관된 디자인 시스템
- **XML Layouts**: 기존 뷰 시스템 활용

### Architecture & Libraries

#### 네트워킹
- **Retrofit 2.9.0**: REST API 통신
- **Gson Converter**: JSON 데이터 파싱

#### 이미지 로딩
- **Glide 4.16.0**: 효율적인 이미지 캐싱 및 로딩

#### Firebase
- **Firebase BOM 33.7.0**
    - Firebase Analytics
    - Firebase Cloud Messaging (FCM)
    - Firestore (데이터 저장)

#### UI Components
- **RecyclerView**: 리스트 렌더링
- **ConstraintLayout**: 복잡한 레이아웃 구성
- **Material Components**: Material Design 3 컴포넌트

#### Jetpack Compose
- **Compose BOM 2024.02.00**
- **Material3**: Compose용 Material Design 3
- **Material Icons Extended**: 확장 아이콘 세트

---

## 🔧 설치 및 실행

### 사전 요구사항

- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17 이상
- Android SDK API 34

### 설치 방법

1. **저장소 클론**
```bash
git clone https://github.com/yourusername/madcamp_week1.git
cd madcamp_week1
```

2. **Firebase 설정**
    - Firebase Console에서 프로젝트 생성
    - `google-services.json` 파일을 `app/` 디렉토리에 추가
    - FCM 설정 완료

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
    "id": "video_001",
    "title": "제목",
    "author": "작성자",
    "views": 1234567,
    "likes": 50000,
    "url": "https://tiktok.com/@user/video/123",
    "imageFile": "https://cdn.tiktok.com/thumb.jpg",
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
    "id": "video_002",
    "title": "챌린지 제목",
    "author": "작성자",
    "views": 987654,
    "likes": 30000,
    "url": "https://tiktok.com/@user/video/456",
    "imageFile": "https://cdn.tiktok.com/thumb2.jpg",
    "category": "challenge"
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

---

## 👥 개발팀

- **프로젝트 기간**: KAIST 몰입캠프 Week 1
- **개발 환경**: Android Studio, Kotlin
