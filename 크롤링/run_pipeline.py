import subprocess
import sys

def run_script(script_name):
    print(f"▶️ 실행 중: {script_name}...")
    result = subprocess.run([sys.executable, script_name], capture_output=False)
    if result.returncode != 0:
        print(f"❌ {script_name} 실행 실패. 파이프라인을 중단합니다.")
        return False
    print(f"✅ {script_name} 완료.\n")
    return True

if __name__ == "__main__":
    # 사용자님이 정하신 순서대로 실행
    pipeline = [
        "crawling.py",
        "cnn_pic_dec.py",
        "google_language_detector.py",
        "top10_filter.py"
    ]

    for script in pipeline:
        if not run_script(script):
            break
    else:
        print("🎉 모든 데이터 정제 파이프라인이 성공적으로 끝났습니다!")