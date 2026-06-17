# UpgradeGUI

Paper 1.20.1용 GUI 강화 플러그인입니다.

## 기능
- `/강화` GUI 열기
- 왼쪽 슬롯에 장비, 오른쪽 슬롯에 강화권 배치
- 성공 / 하락 / 0강 초기화 확률 설정
- 강화 단계 PDC 저장
- 이름 / 로어 / CustomModelData 단계별 반영
- 단계별 성공 시 서버 명령 실행 지원
- Citizens NPC 우클릭으로 GUI 열기 지원

## 명령어
- `/강화`
- `/강화 reload`
- `/강화 list`
- `/강화 give <player> <ticket> [amount]`

## 설정 포인트
- `level-rules.<현재강화단계>` 에서 성공/하락/초기화 확률 조절
- `success-commands` 에서 특정 강화 성공 시 커스텀 아이템 지급 명령 연동 가능
- `custom-model-data-by-level` 로 리소스팩 모델 치환 가능
