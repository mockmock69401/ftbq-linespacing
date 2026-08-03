# FTB Quests Line Spacing & Chapter Cards (Fabric, MC 1.20.1)

FTB Quests(2001.x) UI 가독성 개선 모드입니다.

## 기능
1. **퀘스트 설명 행간** — 하드코딩된 setSpacing(9)를 교체 (기본 11), 문단 간격 1 → 3
2. **카드형 챕터 목록** — 챕터마다 테두리 있는 카드, 아이콘 수직 중앙 정렬,
   제목 아래 진행률 % 표시(완료 시 초록색), 선택된 챕터는 금색 테두리 + 왼쪽 강조 바
3. **제목/부제목 크기 조절** — 퀘스트 제목·부제목 글자 크기(스케일) 조절 (기본 제목 1.45배)
4. **제목 세로 여백 조절** — 제목 위·아래 여백(밴드) 조절, 제목 줄 좌·우 아이콘(퀘스트 아이콘, 고정/닫기) 세로 중앙 정렬
5. **설명 패딩 조절** — 부제목·설명 텍스트 영역의 좌우/위/아래 여백 조절 (스크롤 없이 창 높이로 반영)
6. **화살표 들여쓰기** — ◄► 의존성/후속 퀘스트 화살표를 창 가장자리에서 안쪽으로 들여쓰기
7. **제목 줄 아이콘 x 패딩** — 제목 좌·우 아이콘(퀘스트 아이콘·고정/닫기)을 가장자리에서 안쪽으로 들여쓰기
8. **창 높이 자동 맞춤 / 최소 높이** — 제목 크기·여백에 맞춰 창 높이를 재계산(스크롤/잘림 방지), 짧은 퀘스트용 최소 높이 지정

## 설정 (config/ftbq-linespacing.properties, 게임 재시작 필요)
| 키 | 기본값 | 설명 |
|---|---|---|
| line-spacing | 11 | 설명 텍스트 행간 (9~20) |
| paragraph-gap | 3 | 설명 문단 간격 (0~12) |
| chapter-cards | true | 카드형 챕터 목록 on/off |
| card-height | 26 | 카드 높이 (20~40) |
| card-gap | 4 | 카드 사이 간격 (0~10) |
| show-progress | true | 진행률 % 표시 |
| title-scale | 1.45 | 퀘스트 제목 글자 크기 배율 (0.5~2.0, 1.0 = 기본) |
| subtitle-scale | 1.0 | 퀘스트 부제목 글자 크기 배율 (0.5~2.0, 1.0 = 기본) |
| title-padding-y | 8 | 제목 위·아래 세로 여백 px (0~20, 기본 4) |
| desc-padding-x | 8 | 부제목·설명 영역 좌우 여백 px (0~40) |
| desc-padding-top | 2 | 부제목·설명 영역 위쪽 여백 px (0~40) |
| desc-padding-bottom | 8 | 부제목·설명 영역 아래쪽 여백 px (0~40, 기본 2) |
| arrow-padding-x | 4 | ◄► 화살표를 창 가장자리에서 안쪽으로 들이는 px (0~30) |
| title-icon-padding-x | 4 | 제목 줄 좌·우 아이콘(퀘스트 아이콘·고정/닫기)을 가장자리에서 안쪽으로 들이는 px (0~30) |
| min-window-height | 120 | 퀘스트 창 최소 높이 px (0~400, 0 = 끔). 부제목·본문 없는 짧은 퀘스트가 잘리지 않도록 함 |

## 빌드
JDK 17 필요. FTB Quests/FTB Library를 컴파일 전용 의존성으로 받아오므로 인터넷 연결 필요.

```
gradlew.bat build     # 리눅스/맥: ./gradlew build
```

결과물: build/libs/ftbq-linespacing-1.0.0.jar (-sources 아닌 쪽)

## 대상 버전
- Minecraft 1.20.1 / Fabric Loader 0.14.21+
- FTB Quests Fabric 2001.x (v2001.3.1 소스 기준) / FTB Library 2001.x

모든 인젝터가 require = 0이라 대상 코드가 바뀌면 크래시 대신 조용히 비활성화됩니다.
(카드 렌더링 관련 인젝터는 defaultRequire=1이지만 chapter-cards=false로 끌 수 있음)
