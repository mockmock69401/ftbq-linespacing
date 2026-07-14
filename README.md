# FTB Quests Line Spacing & Chapter Cards (Fabric, MC 1.20.1)

FTB Quests(2001.x) UI 가독성 개선 모드입니다.

## 기능
1. **퀘스트 설명 행간** — 하드코딩된 setSpacing(9)를 교체 (기본 11), 문단 간격 1 → 3
2. **카드형 챕터 목록** — 챕터마다 테두리 있는 카드, 아이콘 수직 중앙 정렬,
   제목 아래 진행률 % 표시(완료 시 초록색), 선택된 챕터는 금색 테두리 + 왼쪽 강조 바

## 설정 (config/ftbq-linespacing.properties, 게임 재시작 필요)
| 키 | 기본값 | 설명 |
|---|---|---|
| line-spacing | 11 | 설명 텍스트 행간 (9~20) |
| paragraph-gap | 3 | 설명 문단 간격 (0~12) |
| chapter-cards | true | 카드형 챕터 목록 on/off |
| card-height | 26 | 카드 높이 (20~40) |
| card-gap | 4 | 카드 사이 간격 (0~10) |
| show-progress | true | 진행률 % 표시 |

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
