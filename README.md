## 🚢 배포 방법

### 1. APK 빌드

```
Build → Generate Signed Bundle / APK
→ APK 선택
→ 키 생성 또는 선택
→ Release 빌드
```

### 2. 주의사항

- `sync_json.bat` 같은 개발 도구는 APK에 포함 안 됨
- 각 사용자의 출석 데이터는 각자 기기에 저장됨
- 서버 필요 없음 (로컬 저장소만 사용)

---

## 📊 동작 흐름

```
사용자
  ↓
앱 실행 (MainActivity)
  ↓
AttendanceManager.checkTodayAttendance()
  ↓
JSON 파일 확인
  ↓
오늘 출석 여부 확인
  ↓
├─ 이미 출석: Toast "이미 출석했습니다"
└─ 미출석: 출석 기록 저장 → Toast "✅ 출석 완료!"
  ↓
출석체크 페이지 (AttendanceCheckActivity)
  ↓
달력에서 날짜 선택
  ↓
JSON에서 해당 날짜 데이터 조회
  ↓
├─ 데이터 있음: "✅ 출석 완료" + 시간 표시
└─ 데이터 없음: 아무것도 표시 안 함
```

---

## 📚 참고 사항

### 파일 위치 정리

| 위치 | 용도 | 자동 업데이트? |
|------|------|---------------|
| **에뮬레이터 내부** | 앱 실행 중 실제 사용 | ✅ 자동 |
| **PC 프로젝트 assets** | 초기 템플릿, Git 관리 | ❌ 수동 (sync_json.bat) |

### 핵심 개념

- **assets 폴더**: 읽기 전용 초기 리소스
- **내부 저장소**: 앱 실행 중 읽기/쓰기 가능
- **동기화**: 필요 시 수동으로 PC로 복사

---

## 🔗 유용한 명령어 모음

### **캐시 완전 삭제**
```powershell
Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force; Remove-Item -Recurse -Force $env:USERPROFILE\.gradle\caches\8.11.1\transforms -ErrorAction SilentlyContinue; Remove-Item -Recurse -Force C:\Users\logan\AndroidStudioProjects\MadCamp-week1\.gradle -ErrorAction SilentlyContinue
```

### **앱 완전 재설치**
```powershell
.\adb uninstall com.example.madcamp_week1
```

### **에뮬레이터 파일 확인**
```powershell
.\adb shell cat /data/user/0/com.example.madcamp_week1/files/storage/attendance.json
```

### **JSON 파일 PC로 가져오기**
```powershell
.\adb pull /data/user/0/com.example.madcamp_week1/files/storage/attendance.json .
```

---

## ✅ 최종 점검

### 앱 실행 전

```
□ Android Studio 최신 버전
□ JDK 17 또는 21 설정
□ Gradle 8.11.1
□ 에뮬레이터 날짜 확인
□ assets/storage/attendance.json 존재 (빈 배열 [])
```

### 빌드 성공 확인

```
□ Gradle Sync 성공
□ Build 성공 (Ctrl+F9)
□ Run 버튼 활성화
□ 앱 실행 시 출석 Toast 표시
□ 출석체크 페이지에서 오늘 날짜 "출석 완료" 표시
```

---

## 📞 문제 발생 시

1. **Gradle 에러**: Invalidate Caches & Restart
2. **캐시 손상**: PC 재부팅 후 transforms 삭제
3. **날짜 문제**: 에뮬레이터 설정에서 날짜 확인
4. **파일 동기화**: sync_json.bat 실행

---

**작성일:** 2026-01-11  
**프로젝트:** MadCamp Week 1  
**개발자:** 김건희
