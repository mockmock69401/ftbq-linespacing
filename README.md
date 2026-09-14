# Chapter Cards UI with Spacing (Fabric, MC 1.20.1 / 1.21.1)

FTB Quests(2001.x / 2101.x)의 UI 가독성을 개선하는 비공식 애드온입니다.
Feed The Beast Ltd와 제휴하거나 승인받은 모드가 아닙니다.


https://github.com/user-attachments/assets/a3638bdb-35f6-40fe-8133-16b0435d848c


<br/>

## 기능
1. **퀘스트 설명 행간** — 하드코딩된 setSpacing(9)를 교체 (기본 12), 문단 간격(paragraph-gap)도 별도로 조절 가능
2. **카드형 챕터 목록** — 챕터마다 테두리 있는 카드, 아이콘 수직 중앙 정렬,
   제목 아래 진행률 % 표시(완료 시 초록색), 선택된 챕터는 금색 테두리 + 왼쪽 강조 바
3. **제목/부제목 크기 조절** — 퀘스트 제목·부제목 글자 크기(스케일) 조절 (기본 제목 1.45배)
4. **제목 세로 여백 조절** — 제목 위·아래 여백(밴드) 조절, 제목 줄 좌·우 아이콘(퀘스트 아이콘, 고정/닫기) 세로 중앙 정렬
5. **설명 패딩 조절** — 부제목·설명 텍스트 영역의 좌우/위/아래 여백 조절 (스크롤 없이 창 높이로 반영)
6. **화살표 들여쓰기** — ◄► 의존성/후속 퀘스트 화살표를 창 가장자리에서 안쪽으로 들여쓰기
7. **제목 줄 아이콘 x 패딩** — 제목 좌·우 아이콘(퀘스트 아이콘·고정/닫기)을 가장자리에서 안쪽으로 들여쓰기
8. **창 높이 자동 맞춤 / 최소 높이** — 제목 크기·여백에 맞춰 창 높이를 재계산(스크롤/잘림 방지), 짧은 퀘스트용 최소 높이 지정
<br/>

## 설정 (config/ftbq-linespacing.properties, 게임 재시작 필요)
설정 파일은 **없을 때만** 아래 순서대로 분류와 키별 설명 주석이 달린 기본값으로 생성됩니다.
이미 있는 파일은 모드가 다시 쓰지 않으므로 직접 수정한 값과 주석이 그대로 유지되고, 파일에 없는 키는 기본값이 적용됩니다.
새 버전에서 추가된 옵션과 설명을 파일에 받으려면 파일을 지우고 게임을 다시 실행하세요.

| 분류 | 키 | 기본값 | 설명 |
|---|---|---|---|
| 퀘스트 창: 제목 | title-scale | 1.45 | 퀘스트 제목 글자 크기 배율 (0.5~2.0, 1.0 = 기본) |
| | title-padding-y | 8 | 제목 위·아래 세로 여백 px (0~20, 기본 4) |
| | title-icon-padding-x | 4 | 제목 줄 좌·우 아이콘(퀘스트 아이콘·고정/닫기)을 가장자리에서 안쪽으로 들이는 px (0~30) |
| 퀘스트 창: 부제목·설명 | subtitle-scale | 1.0 | 퀘스트 부제목 글자 크기 배율 (0.5~2.0, 1.0 = 기본) |
| | line-spacing | 12 | 설명 텍스트 행간 (9~20) |
| | paragraph-gap | 0 | 설명 문단 간격, line-spacing에 더해지는 여분 px (0~12, 0 = 줄바꿈과 동일 간격) |
| | desc-word-wrap | true | 부제목·설명을 공백 기준으로만 줄바꿈 (한글 등 CJK 텍스트가 양쪽 정렬처럼 보이는 것을 방지). false면 FTBQ 기본 줄바꿈 사용 |
| | desc-padding-x | 8 | 부제목·설명 영역 좌우 여백 px (0~40) |
| | desc-padding-top | 2 | 부제목·설명 영역 위쪽 여백 px (0~40) |
| | desc-padding-bottom | 8 | 부제목·설명 영역 아래쪽 여백 px (0~40, 기본 2) |
| 퀘스트 창: 테두리 | window-corner-radius | 6 | 퀘스트 창 모서리 둥글기 px (0~16, 0 = 직각 모서리) |
| | min-window-height | 120 | 퀘스트 창 최소 높이 px (0~400, 0 = 끔). 부제목·본문 없는 짧은 퀘스트가 잘리지 않도록 함 |
| | arrow-padding-x | 4 | ◄► 화살표를 창 가장자리에서 안쪽으로 들이는 px (0~30) |
| 챕터 목록: 카드 | chapter-cards | true | 카드형 챕터 목록 on/off (false면 아래 챕터 목록 옵션 전부 무시) |
| | card-height | 26 | 카드 높이 (20~40) |
| | card-gap | 4 | 카드 사이 간격 (0~10) |
| | card-min-width | 170 | 카드 최소 너비 px (100~400, 패널이 이보다 좁으면 늘어남) |
| | show-progress | true | 진행률 % 표시 |
| | progress-bar | true | 카드 배경에 은은한 진행률 바 표시 |
| 챕터 목록: 애니메이션 | transition-ms | 120 | hover/click·슬라이드 애니메이션 시간 ms (0~500, 0 = 즉시 전환/끔) |
| | card-scale | true | 카드에 hover/click 시 살짝 확대/축소 효과 적용 (transition-ms > 0일 때) |
<br/>

## 지원 버전
| Minecraft | Fabric Loader | FTB Quests (Fabric) | FTB Library (Fabric) | Java |
|---|---|---|---|---|
| 1.20.1 | 0.14.21+ | 2001.4.14 ~ 2001.4.x | 2001.2.9 ~ 2001.2.x | 17+ |
| 1.21.1 | 0.16.0+ | 2101.1.35 ~ 2101.1.x | 2101.1.36 ~ 2101.1.x | 21+ |

범위 밖의 FTB 버전에서는 Fabric Loader가 실행 전에 호환되지 않는다고 안내합니다.

<br/>

## 빌드
FTB Quests/FTB Library를 컴파일 전용 의존성으로 받아오므로 첫 빌드에는 인터넷 연결이 필요합니다.
Java 소스는 두 버전이 공유하고, `fabric.mod.json` 등 리소스만 버전별로 따로 있습니다.

```
# 1.20.1 (JDK 17)
gradlew.bat build
# → build/libs/ftbq-linespacing-1.0.0.jar

# 1.21.1 (JDK 21, 없으면 Gradle이 자동으로 받음)
versions\1.21.1\gradlew.bat -p versions\1.21.1 build
# → versions/1.21.1/build/libs/ftbq-linespacing-1.0.0+1.21.1.jar
```

리눅스/맥은 `./gradlew`, `versions/1.21.1/gradlew`를 사용하세요. `-sources`가 붙지 않은 jar가 배포용입니다.

<br/>

## 호환성 안전장치
간격·레이아웃 관련 인젝터는 `require = 0`이라 대상 코드가 바뀌어도 크래시 대신 조용히 비활성화됩니다.
챕터 카드 렌더링 인젝터와 `@Accessor`는 대상이 없으면 로드 시점에 실패하므로(설정 파일보다 먼저 적용되어
`chapter-cards=false`로도 막을 수 없음), `fabric.mod.json`의 FTB 버전 범위로 검증된 버전에서만 로드되도록 제한합니다.
