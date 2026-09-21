# 스텝락 (StepLock)

> 걸음 수 · 수면 시간 · 집중 타이머 중 하나를 채워야 쇼츠·릴스·틱톡의 잠금이 풀리는 습관 관리 앱.
> Kotlin + Jetpack Compose로 만든 **UI 레이어 구현**입니다.

디자인 시안(OKLCH 토큰 기반 HTML 프로토타입)을 Compose로 이식하면서, 색·간격·타이포·터치 영역을
토큰으로 정리하고 화면을 재사용 컴포저블 단위로 분리했습니다.

**웹 미리보기** — [`docs/index.html`](docs/index.html)에 다섯 화면을 Android 프레임으로 옮겨 두었습니다.
로그인 진입점 문구, 비밀번호 표시 토글, 설정의 토글·스테퍼가 실제로 동작하고 홈 게이지와 잠금 화면
문구까지 함께 갱신됩니다. 저장소 Settings → Pages에서 `main` 브랜치 `/docs` 폴더를 켜면
링크로 바로 열 수 있습니다.

---

## 화면

| 화면 | 역할 | 파일 |
| --- | --- | --- |
| **Login** | 앱 첫 화면. 밑줄형 입력 · 로그인 유지 · 소셜 3종 · 게스트 진입 | [`LoginScreen.kt`](app/src/main/java/com/steplock/app/ui/screens/LoginScreen.kt) |
| **Onboarding** | 잠금 해제 조건 3가지 안내 + 손쉬운 사용 권한 요청 | [`OnboardingScreen.kt`](app/src/main/java/com/steplock/app/ui/screens/OnboardingScreen.kt) |
| **Home** | 오늘의 달성 현황, 잠금 상태 배너, 차단 중인 앱 목록 | [`HomeScreen.kt`](app/src/main/java/com/steplock/app/ui/screens/HomeScreen.kt) |
| **Settings** | 조건별 토글 + 목표값 스테퍼, 차단할 앱 선택 | [`SettingsScreen.kt`](app/src/main/java/com/steplock/app/ui/screens/SettingsScreen.kt) |
| **Lock** | 차단 앱 실행 시 덮이는 전체 화면 오버레이 (다크 팔레트) | [`LockOverlayScreen.kt`](app/src/main/java/com/steplock/app/ui/screens/LockOverlayScreen.kt) |

화면 이동: `Login → Onboarding → Home`, 홈에서 설정·잠금 오버레이로 진입합니다.
온보딩을 마친 기기는 로그인을 건너뛰고 홈으로 시작합니다 (`StepLockViewModel.startDestination`).

각 화면 파일 하단에 `@Preview`가 있어 Android Studio에서 412×892 프레임으로 바로 확인할 수 있습니다.

---

## 디자인 시스템

### 색상 — OKLCH로 설계, sRGB로 변환

시안은 OKLCH로 정의했고 Compose에서 쓸 수 있도록 sRGB로 변환했습니다.
원본 토큰은 [`Color.kt`](app/src/main/java/com/steplock/app/ui/theme/Color.kt)에 주석으로 남겨
두어 시안과 대조할 수 있습니다.

| 역할 | OKLCH | sRGB |
| --- | --- | --- |
| 배경 | `oklch(96% 0.012 90)` | `#F5F2E9` |
| 서피스 | `oklch(99% 0.006 90)` | `#FDFCF7` |
| 서피스(보조) | `oklch(93% 0.02 90)` | `#EDE8D9` |
| 보더 | `oklch(88% 0.015 90)` | `#DBD7CD` |
| 본문 | `oklch(23% 0.02 260)` | `#171D26` |
| 보조 텍스트 | `oklch(50% 0.02 260)` | `#5D646F` |
| 브랜드(그린) | `oklch(58% 0.13 165)` | `#009267` |
| 브랜드(진한톤) | `oklch(40% 0.115 165)` | `#005A38` |
| 강조(앰버·잠금) | `oklch(70% 0.15 55)` | `#E48233` |
| 다크 배경 | `oklch(19% 0.02 260)` | `#0F141D` |
| 다크 서피스 | `oklch(25% 0.02 260)` | `#1C222B` |

- 배경은 순수 흰색·검정 대신 브랜드 색이 은은하게 섞인 **틴티드 뉴트럴**을 씁니다.
- 다크 팔레트는 **잠금 오버레이 전용**이라 시스템 다크모드를 따라가지 않습니다.
- 앱 뱃지(쇼츠·릴스·틱톡·엑스)와 소셜 아이콘만 식별 목적으로 각 브랜드 색을 유지합니다.

### 타이포그래피

Noto Sans KR 400/500/700/900. 화면에서 쓰는 스타일을 [`Type.kt`](app/src/main/java/com/steplock/app/ui/theme/Type.kt)에
의미 단위로 모아 두고(`SlText.Greeting`, `SlText.RowTitle` …) `includeFontPadding = false`로
시안의 행간을 맞췄습니다. 폰트는 다운로더블 폰트로 받아오며, ttf를 번들하려면
`res/font`에 넣고 `NotoSansKr` 정의만 교체하면 됩니다.

### 간격 · 형태 · 접근성

- 간격은 **4dp 그리드**(4 · 8 · 12 · 16 · 20 · 24 · 28 · 32)만 사용합니다.
- 라운드는 14~20dp, CTA는 16dp, 로그인 버튼과 칩은 캡슐형입니다.
- 카드를 겹치지 않고 하나의 패널 안에서 구분선과 여백으로 섹션을 나눕니다(`SlPanel`).
- 모든 탭 영역은 **44dp 이상**입니다. 스위치·체크박스·텍스트 링크도 시각 크기와 별개로
  터치 영역을 44dp로 확보했습니다.
- 아이콘은 이모지·비트맵 없이 stroke 벡터로 직접 그렸습니다
  ([`SlIcons.kt`](app/src/main/java/com/steplock/app/ui/components/SlIcons.kt)).
- 문구는 동작을 알 수 있게 씁니다 — "손쉬운 사용 권한 허용하기", "알겠어요, 닫기",
  "로그인 없이 둘러보기".
- 한국어 조사 처리: 잠금 화면 제목은 받침에 따라 은/는을 붙입니다("쇼츠는", "틱톡은").

---

## 재사용 컴포저블

| 컴포저블 | 설명 |
| --- | --- |
| `ProgressRing` | 홈 44dp · 잠금 200dp 공용 원형 게이지. 박스 크기와 링 반지름을 따로 받습니다. |
| `ConditionRow` | 홈의 조건 한 줄. 앞쪽 시각 요소와 뒤쪽 상태를 슬롯으로 받습니다. |
| `ConditionSettingCard` | 토글 + 구분선 + `GoalStepper` 조합. 설정의 조건 카드 3개. |
| `GoalStepper` | −/+ 44dp 버튼과 목표값. 걸음 500보 · 수면 30분 · 세션 1회 단위. |
| `AppListItem` | 앱 뱃지 + 이름/부제 + 상태 슬롯(감지 중 점 / 체크박스). |
| `LockBanner` | 앰버 톤 잠금 상태 배너. 탭하면 설정으로 이동합니다. |
| `StatusChip` | 잠금 화면 미니 상태 칩. 달성 / 진행 중 / 미시작 3상태. |
| `UnderlineTextField` | 밑줄형 입력. 포커스 시 밑줄이 브랜드 그린, 비밀번호는 표시 토글 내장. |
| `SocialLoginButton` | 구글 · 카카오 · 애플 44dp 원형 버튼. |
| `SlSwitch` · `CheckboxMark` | 시안 규격(52×32 트랙, 24dp 체크박스)에 맞춘 선택 컨트롤. |
| `BottomNavBar` | 홈 · 통계 · 설정 탭. 라벨 표시를 끌 수 있습니다. |
| `SlPanel` · `SectionLabel` · `PrimaryButton` · `TextLink` · `IconTile` · `AppBadge` | 공통 레이아웃 조각. |

---

## 데이터 · 상태 설계

로그인 전에도 기기별로 기록이 쌓이고, 로그인 후 서버 계정에 귀속시킬 수 있도록 모델을 준비했습니다
([`Models.kt`](app/src/main/java/com/steplock/app/data/Models.kt)).

- `LockSettings` · `DailyStat` — `deviceUuid: String`과 `accountId: String?`를 함께 보관.
  로그인 성공 시 `deviceUuid` 레코드를 계정에 귀속(claim)시키는 마이그레이션 훅용입니다.
- `AuthState` — `Unknown` · `Guest` · `SignedIn(accountId)`.
- `StepLockViewModel` — 목표값·토글·차단 앱 선택을 들고 있어, 설정에서 걸음 목표를 바꾸면
  홈 게이지와 잠금 화면 문구가 함께 갱신됩니다.

---

## 기술 스택

- Kotlin 2.0 · Jetpack Compose (Material 3) · Navigation Compose
- Gradle KTS + 버전 카탈로그 (`gradle/libs.versions.toml`)
- minSdk 26 / targetSdk 35 · edge-to-edge

```
app/src/main/java/com/steplock/app
├── MainActivity.kt
├── data/            # LockSettings · DailyStat · BlockedApp · AuthState · 데모 데이터
├── navigation/      # Route · StepLockNavHost
└── ui/
    ├── components/  # 재사용 컴포저블 + stroke 아이콘 세트
    ├── screens/     # 5개 화면
    ├── theme/       # 색 · 타이포 · 치수 토큰
    └── util/        # 숫자·시간·조사 포맷
```

## 실행

```bash
./gradlew assembleDebug
```

Android Studio에서 열면 각 화면의 `@Preview`로 레이아웃을 바로 볼 수 있습니다.
빌드 없이 화면만 보려면 `docs/index.html`을 브라우저에서 열면 됩니다.

## 구현 범위

지금은 **화면·레이아웃·UI 상태**까지입니다. 아래는 아직 연결하지 않았습니다.

- 실제 인증 (Firebase Auth, 구글·카카오·애플 SDK) — 버튼과 상태만 준비
- 손쉬운 사용(Accessibility) 서비스 기반 앱 감지와 오버레이 표시
- 걸음 수·수면 데이터 연동 (Health Connect / 센서)
- DataStore 영구 저장 — 현재 목표값은 앱 실행 중에만 유지됩니다
- 통계 탭 화면
