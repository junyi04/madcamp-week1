# 📑 TikTok Discover Trends Crawler
이 프로젝트는 Playwright의 네트워크 가로채기(Network Interception) 기능을 활용하여 틱톡(TikTok)의 검색 및 탐색(Discover) 페이지에서 실시간 트렌드 영상 데이터를 수집하는 도구입니다.
### ✨ 핵심 기능 (Key Features)
- API 가로채기: 브라우저가 서버와 주고받는 실제 JSON 응답을 낚아채어 데이터의 정확도가 높습니다.
- 실시간 정제: 수많은 데이터 중 필요한 정보(제목, 작성자, 조회수, 좋아요, URL)만 골라 추출합니다.
- 중복 수집 방지: 세션 내 seen_video_ids를 활용하여 동일한 영상이 여러 번 저장되는 것을 방지합니다.
- 자동 스크롤: 설정된 횟수만큼 마우스 휠을 자동으로 내려 추가 데이터를 로드합니다.
- 경량 저장: meme_data.json 파일에 JSONL(JSON Lines) 형식으로 한 줄씩 저장하여 데이터 안정성을 확보합니다.
### 🛠 기술 스택 (Tech Stack)
- 기술용도Python 3.10+메인 프로그래밍 언어
- Playwright브라우저 자동화 및 네트워크 트래픽 분석
- Asyncio비동기 처리 및 효율적인 리소스 관리
- JSON데이터 파싱 및 저장 포맷
## 🚀 시작하기 (Quick Start)
1. 환경 구성 
#### 이 프로젝트는 가상환경(.venv) 사용을 권장합니다.
#####
#가상환경 활성화 (Windows 기준)
~~~
.venv\Scripts\activate
~~~
#필수 라이브러리 설치
~~~
pip install playwright
~~~
#브라우저 엔진 설치
~~~
playwright install chromium
~~~
2. 실행 방법
#### 프로젝트 루트 폴더에서 파이썬 파일을 실행합니다.
~~~
python crolling.py
~~~
## 📊 데이터 추출 항목 (Data Schema)
#### 수집된 데이터는 meme_data.json에 다음과 같은 구조로 저장됩니다.
|필드명|설명|비고|
|---|---|---|
|title | 영상의 설명(제목) | item.desc 추출
|author | 영상 제작자의 닉네임 | author.nickname 추출
|views | 누적 조회수 | stats.playCount
|likes | 누적 좋아요 수 | tats.diggCount
|url | 릴스 바로가기 링크 | 고유 ID를 포함한 절대 경로