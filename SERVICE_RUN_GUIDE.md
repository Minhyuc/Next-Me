# Next-Me 전체 서비스 구동 및 API 사용 예시

## 1. 서비스 구성

| 서비스 | 포트 | 역할 |
|---|---:|---|
| gateway | 8080 | 모든 외부 API 요청의 진입점 |
| login-service | 8081 | 회원가입, 이메일 인증, 로그인, JWT 발급 |
| user-service | 8082 | 프로필과 루틴 선호도 관리 |
| ai-chat-service | 8083 | 피부·운동·루틴 AI 대화 |
| routine-service | 8084 | AI 일일 루틴, 완료율, 초기화, 일정 조정, 완료 이력 |
| personal-color-service | 8085 | 퍼스널컬러 기반 화장품·오늘의 옷 추천 및 이력 |
| character-service | 8086 | 루틴 100% 완료 캐릭터 보상, 보유 현황, 도감 |
| user-care-service | 8087 | 프로필 기반 세안·스킨케어 추천·주의 성분 |

클라이언트는 특별한 내부 테스트가 아니라면 각 서비스 포트에 직접 요청하지 않고 Gateway의 `http://localhost:8080`을 사용한다.

## 2. 사전 준비

### Java 및 MySQL

- JDK 17
- MySQL 실행
- 필요하면 최초 1회 기본 DB 생성

```sql
CREATE DATABASE IF NOT EXISTS NMLoginData;
CREATE DATABASE IF NOT EXISTS NMUserData;
CREATE DATABASE IF NOT EXISTS NMAiChatData;
CREATE DATABASE IF NOT EXISTS NMRoutineData;
CREATE DATABASE IF NOT EXISTS NMPersonalColorData;
CREATE DATABASE IF NOT EXISTS NMCharacterData;
CREATE DATABASE IF NOT EXISTS NMUserCareData;
```

위 데이터베이스는 각 서비스를 실행하기 전에 MySQL에 생성되어 있어야 한다.

## 가비아 배포 환경 변수

배포용 예시는 프로젝트 루트의 `.env.production.example`에 정리되어 있다.
실제 비밀번호와 API 키가 들어간 파일은 Git에 커밋하지 않는다.

- Spring Boot와 MySQL을 같은 가비아 서버에서 실행하면 DB URL의 `localhost`를 유지한다.
- 가비아 DB 호스팅 또는 별도 DB 서버를 사용하면 각 `*_DB_URL`의 호스트를 제공받은 주소로 바꾼다.
- 모든 Spring Boot 서비스를 한 서버에서 직접 실행하면 `*_SERVICE_URL`도 `localhost`를 유지한다.
- Docker Compose로 실행하면 `localhost` 대신 각 컨테이너 서비스 이름을 사용한다.
- 외부에는 Gateway 포트만 열고 8081~8087과 MySQL 3306은 내부 접근만 허용한다.
- `CREATE_JWT`는 모든 서비스에 반드시 같은 값을 설정한다.

가비아 서버에서 사용할 주요 환경 변수:

```text
LOGIN_DB_URL
USER_DB_URL
AI_CHAT_DB_URL
ROUTINE_DB_URL
PERSONAL_COLOR_DB_URL
DB_USERNAME
MYSQL_PASSWORD
CREATE_JWT
OPENAI_API_KEY
GOOGLE_APP_PASSWORD
USER_SERVICE_URL
CHARACTER_SERVICE_URL
CHARACTER_INTERNAL_KEY
USER_CARE_DB_URL
USER_CARE_SERVICE_URL
```

### .env

`Next-Me/Next-Me/.env`에 아래 항목을 설정한다. 실제 비밀값은 Git에 커밋하지 않는다.

```dotenv
MYSQL_PASSWORD=MySQL_root_비밀번호
GOOGLE_APP_PASSWORD=Gmail_앱_비밀번호
CREATE_JWT=32바이트_이상의_충분히_긴_비밀키
OPENAI_API_KEY=OpenAI_API_키
```

## 3. 터미널에서 전체 서비스 실행

터미널을 8개 열고 아래 순서로 실행한다.

### 터미널 1: user-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :user-service:bootRun
```

### 터미널 2: login-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :login-service:bootRun
```

로그인 시 `user-service`를 호출하므로 `user-service`를 먼저 실행한다.

### 터미널 3: ai-chat-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :ai-chat-service:bootRun
```

### 터미널 4: routine-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :routine-service:bootRun
```

### 터미널 5: personal-color-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :personal-color-service:bootRun
```

### 터미널 6: character-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :character-service:bootRun
```

`routine-service`가 마지막 항목 완료 시 `character-service`에 보상을 요청하므로 두 서비스를 모두 실행해야 한다.

### 터미널 7: user-care-service

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/Next-Me
./gradlew :user-care-service:bootRun
```

### 터미널 8: gateway

```bash
cd /Users/yunminhyuck/Documents/Codex/2026-08-13/new-chat/Next-Me/gateway
./gradlew bootRun
```

IntelliJ에서는 Gradle 새로고침 후 다음 Spring Boot 클래스를 각각 실행해도 된다.

- `UserServiceApplication`
- `LoginServiceApplication`
- `AiChatServiceApplication`
- `RoutineServiceApplication`
- `PersonalColorServiceApplication`
- `CharacterServiceApplication`
- `UserCareServiceApplication`
- `GatewayApplication`

## 4. 로그인 및 JWT 준비

신규 사용자는 이메일 인증 후 회원가입한다.

```bash
curl -X POST http://localhost:8080/auth/email/send \
  -H 'Content-Type: application/json' \
  -d '{"email": "test@example.com"}'
```

이메일로 받은 인증 코드를 확인한다.

```bash
curl -X POST http://localhost:8080/auth/email/verify \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "test@example.com",
    "code": "123456"
  }'
```

회원가입:

```bash
curl -X POST http://localhost:8080/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "testuser",
    "password": "testpassword",
    "name": "홍길동",
    "email": "test@example.com"
  }'
```

이미 가입된 사용자는 Gateway로 로그인한다.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "testuser",
    "password": "testpassword"
  }'
```

응답 예시:

```json
{
  "userId": 8,
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "testuser",
  "name": "홍길동",
  "email": "test@example.com"
}
```

이후 모든 인증 API의 헤더에 토큰을 넣는다.

```text
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

터미널에서 토큰을 변수로 저장하면 예시를 그대로 사용하기 편하다.

```bash
TOKEN='로그인_응답의_accessToken'
```

## 5. 프로필 상태 확인 및 생성

로그인 직후 프론트는 다음 API로 프로필 상태를 확인한다. 프로필 DB 행이 아직 없어도 빈 프로필을 생성하여 `200 OK`로 반환한다.

```bash
curl http://localhost:8080/users/me \
  -H "Authorization: Bearer $TOKEN"
```

최초 가입 후 아무 프로필 값도 작성하지 않은 응답 예시:

```json
{
  "userId": 8,
  "profileEmpty": true,
  "profileCompleted": false,
  "onboardingStatus": "INCOMPLETE",
  "onboardingMode": null,
  "onboardingStep": null,
  "nickname": null,
  "gender": null,
  "ageGroup": null
}
```

프론트 분기 기준:

- `profileEmpty=true`: 최초 프로필 작성 화면으로 이동
- `profileEmpty=false`, `profileCompleted=false`: 작성 중인 프로필 이어서 진행
- `profileCompleted=true`: 메인 서비스로 이동

프로필 생성:

```bash
curl -X POST http://localhost:8080/users/me/profile \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "nickname": "민혁",
    "gender": "MALE",
    "ageGroup": 20,
    "workDay": "월,화,수,목,금",
    "leaveTime": "09:00",
    "comeTime": "18:00",
    "workStyle": "사무직",
    "sleepHour": 7,
    "makeupFrequency": "거의 안 함",
    "sunscreenFrequency": "매일",
    "exerciseCount": 3,
    "sweatAmount": "보통",
    "skinType": "복합성",
    "concerns": "피지, 수분 부족",
    "personalColor": "봄 웜",
    "onboardingMode": "DIRECT"
  }'
```

프로필 부분 수정은 `PATCH /users/me`를 사용한다. 문자열의 앞뒤 공백은 자동으로 제거된다.

```bash
curl -X PATCH http://localhost:8080/users/me \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"nickname": "  새 닉네임  "}'
```

공백만 전송하면 저장하지 않고 `400 Bad Request`를 반환한다.

```json
{
  "message": "nickname은(는) 공백으로 저장할 수 없습니다."
}
```

## 6. AI에게 받고 싶은 루틴 범주 선택

`true`로 지정한 범주만 `routine-service`가 AI에 허용하고, AI 결과도 서버에서 다시 검증한다.

```bash
curl -X PUT http://localhost:8080/users/me/routine-preferences \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "routineCleansing": true,
    "routineSkinCare": true,
    "routinePersonalColor": false,
    "routineSleepWake": true,
    "routineDiet": false,
    "routineExercise": true
  }'
```

## 7. AI 채팅

새 대화는 `conversationId`를 `null`로 전송한다.

```bash
curl -X POST http://localhost:8080/ai-chat/me/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "conversationId": null,
    "message": "야근한 날에는 피부 관리를 어떻게 하면 좋아?"
  }'
```

이전 대화를 이어가려면 응답의 `conversationId`를 다시 사용한다.

```bash
curl -X POST http://localhost:8080/ai-chat/me/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "conversationId": 1,
    "message": "그럼 마스크팩도 하는 게 좋아?"
  }'
```

## 8. AI 일일 루틴 생성 및 초기화 시각 자동 추천

최초 생성 시 AI가 다음을 함께 결정한다.

- 선택된 범주 안에서 수행할 루틴
- 각 루틴의 수행 시각
- 프로필의 근무·수면·기상 패턴에 맞는 초기화 시각
- 이후 일회성 일정 변경이 끝났을 때 돌아갈 기본 루틴 원본

```bash
curl -X POST http://localhost:8080/routines/me/today/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "additionalRequest": "평일에 부담 없이 할 수 있게 만들어줘"
  }'
```

응답 예시:

```json
{
  "routineGenerated": true,
  "planId": 1,
  "routineDate": "2026-08-13",
  "resetTime": "06:30:00",
  "completionPercentage": 0,
  "allCompleted": false,
  "firstCompletedAt": null,
  "coachMessage": "출근 전 활동 시작 시각을 기준으로 초기화하고 선택한 범주만 구성했어요.",
  "items": [
    {
      "id": 1,
      "category": "CLEANSING",
      "title": "아침 세안",
      "description": "미지근한 물로 가볍게 세안하기",
      "scheduledTime": "07:00:00",
      "sortOrder": 0,
      "completed": false,
      "completedAt": null
    }
  ]
}
```

AI가 추천한 초기화 시각은 `routine_settings`에 저장된다. 해당 시각이 지나면 새 일일 계획이 만들어지고 모든 항목의 완료 여부가 `false`로 초기화된다. 서비스가 초기화 시각에 꺼져 있었더라도 다음 조회 때 자동으로 처리된다.

현재 AI 추천 초기화 시각 확인:

```bash
curl http://localhost:8080/routines/me/settings \
  -H "Authorization: Bearer $TOKEN"
```

필요한 경우에만 사용자가 추천값을 수동으로 덮어쓸 수 있다.

```bash
curl -X PUT http://localhost:8080/routines/me/settings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "resetTime": "05:30",
    "zoneId": "Asia/Seoul"
  }'
```

## 9. 오늘 루틴 조회 및 완료 처리

```bash
curl http://localhost:8080/routines/me/today \
  -H "Authorization: Bearer $TOKEN"
```

최초 루틴 생성 전에도 오류 대신 `200 OK`를 반환한다. 프론트는 `routineGenerated`로 생성 버튼과 루틴 목록을 분기한다.

```json
{
  "routineGenerated": false,
  "planId": null,
  "routineDate": "2026-08-14",
  "resetTime": "06:00:00",
  "completionPercentage": 0,
  "allCompleted": false,
  "firstCompletedAt": null,
  "coachMessage": "아직 생성된 루틴이 없습니다. 생성 버튼을 눌러 오늘의 AI 루틴을 만들어주세요.",
  "items": []
}
```

루틴 항목 완료:

```bash
curl -X PATCH http://localhost:8080/routines/me/today/items/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"completed": true}'
```

완료를 취소하려면 `false`를 보낸다.

```bash
curl -X PATCH http://localhost:8080/routines/me/today/items/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"completed": false}'
```

응답의 `completionPercentage`는 완료 항목 수를 기준으로 자동 계산된다.

각 루틴 응답에는 실행 가능 시간과 마감 시간이 함께 반환된다.

```json
{
  "scheduledTime": "19:00:00",
  "completionWindowMinutes": 30,
  "availableAt": "2026-08-13T19:00:00",
  "deadlineAt": "2026-08-13T19:30:00",
  "completed": false,
  "expired": false,
  "editable": true
}
```

`availableAt` 전에는 완료할 수 없고, 미완료 상태로 `deadlineAt`이 지나면 `expired`가
`true`가 되어 이후 완료 상태를 바꿀 수 없다. 선택된 루틴 범주는 모두 최소 한 항목씩
포함되며, 퍼스널컬러만 별도 서비스로 분리된다.

프론트에서 진행률만 가볍게 다시 조회하려면 전체 루틴 응답 대신 아래 API를 사용한다.

```bash
curl http://localhost:8080/routines/me/today/progress \
  -H "Authorization: Bearer $TOKEN"
```

응답 예시:

```json
{
  "planId": 1,
  "routineDate": "2026-08-13",
  "completedCount": 1,
  "totalCount": 4,
  "completionPercentage": 25,
  "allCompleted": false
}
```

체크박스를 변경하는 `PATCH /routines/me/today/items/{itemId}` 응답에도 전체 루틴 목록과
최신 `completionPercentage`가 포함되므로, 일반적으로는 PATCH 응답을 즉시 화면에 반영하고
화면 재진입이나 새로고침 시 이 진행률 API를 호출하면 된다.

마지막 미완료 항목을 `true`로 변경해 완료율이 처음 100%가 되면 보유하지 않은 1~3번
캐릭터 중 하나가 무작위로 지급된다. 이때 PATCH 응답의 `characterReward`를 사용하면
프론트에서 획득 팝업을 즉시 표시할 수 있다.

```json
{
  "completionPercentage": 100,
  "allCompleted": true,
  "characterReward": {
    "rewardProcessed": true,
    "alreadyProcessed": false,
    "newCharacterCollected": true,
    "characterNumber": 2,
    "ownedCount": 1,
    "totalCharacterCount": 3,
    "allCharactersCollected": false,
    "message": "2번 캐릭터를 새로 수집했습니다."
  }
}
```

같은 날짜의 완료 요청이 다시 들어오면 `alreadyProcessed=true`,
`newCharacterCollected=false`가 반환되며 캐릭터를 다시 지급하지 않는다.

## 10. 갑작스러운 일정 변경에 따른 AI 루틴 조정

```bash
curl -X POST http://localhost:8080/routines/me/today/adjust \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "오늘 갑자기 21시까지 야근하게 됐어. 완료한 항목은 유지하고 나머지를 조정해줘."
  }'
```

이미 완료한 항목은 유지하고 미완료 항목을 중심으로 시간과 내용을 조정한다. 하루의 일회성 일정 변경은 평소 생활 패턴으로 추천된 초기화 시각을 변경하지 않는다.
조정 내용은 현재 활성화된 `routineDate`에만 적용된다. 다음 초기화 시각이 지나면 임시 조정본을 복제하지 않고, 최초 생성 시 저장한 기본 루틴으로 돌아가며 완료 상태와 완료율도 초기화된다.

## 11. 전체 완료 기록 및 누적 횟수 조회

모든 항목이 완료되면 해당 날짜와 완료 시각이 한 번만 기록된다.

```bash
curl http://localhost:8080/routines/me/stats \
  -H "Authorization: Bearer $TOKEN"
```

## 12. 퍼스널컬러 AI 대화 및 화면별 추천

`personal-color-service`는 8085 포트와 `NMPersonalColorData` DB를 사용한다.
`user-service`의 기존 온보딩은 변경하지 않는다.
추천 이력은 같은 DB 안의 `outfit_recommendations`(옷)와
`makeup_recommendations`(화장) 테이블에 서로 분리하여 저장한다.
기존 `personal_color_recommendations` 테이블이 있으면 서비스 시작 시 새 테이블로
중복 없이 복사하며, 확인과 복구를 위해 기존 테이블은 자동 삭제하지 않는다.

AI 분석 대화 시작:

```bash
curl -X POST http://localhost:8080/personal-colors/me/analysis/start \
  -H "Authorization: Bearer $TOKEN"
```

응답의 `conversationId`로 대화를 이어간다.

```bash
curl -X POST http://localhost:8080/personal-colors/me/analysis/messages \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "conversationId": 1,
    "message": "햇빛을 받으면 쉽게 붉어지고 실버 액세서리가 잘 어울려요."
  }'
```

판정 완료 후 옷 추천 생성:

```bash
curl -X POST http://localhost:8080/personal-colors/me/outfits/recommendations \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "message": "오늘 회사에 출근할 거야. 날씨는 덥고 단정한 스타일로 추천해줘."
  }'
```

화장 추천은 옷 추천과 별도 화면 및 API로 호출한다.

```bash
curl -X POST http://localhost:8080/personal-colors/me/makeup/recommendations \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"message":"오늘 회사에 출근할 거야. 자연스럽고 단정한 데일리 메이크업을 추천해줘."}'
```

이력은 페이지 단위로 조회한다.

```bash
curl 'http://localhost:8080/personal-colors/me/outfits/recommendations?page=0&size=20' \
  -H "Authorization: Bearer $TOKEN"

curl 'http://localhost:8080/personal-colors/me/makeup/recommendations?page=0&size=20' \
  -H "Authorization: Bearer $TOKEN"
```

## 13. 캐릭터 보유 현황 및 도감

### 현재 보유한 캐릭터 번호 조회

```bash
curl http://localhost:8080/characters/me/collection \
  -H "Authorization: Bearer $TOKEN"
```

응답 예시:

```json
{
  "totalCharacterCount": 3,
  "ownedCount": 2,
  "allCharactersCollected": false,
  "ownedCharacterNumbers": [1, 3]
}
```

### 전체 캐릭터 도감 조회

1번부터 3번까지 항상 순서대로 반환한다. 보유 중이면 `owned=true`, 미보유면
`owned=false`이며 미보유 캐릭터의 `collectedAt`은 `null`이다.

```bash
curl http://localhost:8080/characters/me/catalog \
  -H "Authorization: Bearer $TOKEN"
```

응답 예시:

```json
{
  "totalCharacterCount": 3,
  "ownedCount": 2,
  "allCharactersCollected": false,
  "characters": [
    {
      "characterNumber": 1,
      "owned": true,
      "collectedAt": "2026-08-14T14:20:31"
    },
    {
      "characterNumber": 2,
      "owned": false,
      "collectedAt": null
    },
    {
      "characterNumber": 3,
      "owned": true,
      "collectedAt": "2026-08-16T20:10:05"
    }
  ]
}
```

프론트는 `characterNumber`를 이미지 파일과 연결하고 `owned=false`인 항목에는 잠금 또는
실루엣 이미지를 표시하면 된다. 캐릭터 지급용 `/internal/characters/**` API는
routine-service 전용이므로 프론트나 Postman에서 직접 호출하지 않는다.

## 14. 프로필 기반 피부 관리 성분 추천

`user-care-service`는 8087 포트와 `NMUserCareData` DB를 사용한다. JWT의 현재 사용자를
기준으로 `user-service`에서 완성된 프로필을 조회하며, 피부 타입과 고민을 포함한 최소
정보만 AI 추천 문맥으로 사용한다.

### 새로운 추천 생성

Body 없이 Bearer JWT만 전송한다.

```bash
curl -X POST http://localhost:8080/user-care/me/recommendations \
  -H "Authorization: Bearer $TOKEN"
```

응답은 사용하면 좋은 제품 유형·성분 3개와 주의할 제품 유형·성분 3개를 정확히 반환한다.

```json
{
  "recommendationId": 1,
  "skinType": "민감성",
  "concerns": "붉어짐, 수분 부족",
  "summary": "자극을 줄이고 피부 장벽과 보습을 우선하는 관리가 적합합니다.",
  "recommendedIngredients": [
    {
      "careArea": "CLEANSING",
      "productType": "약산성 젤 클렌저",
      "ingredient": "코코일글루타메이트 계열 세정 성분",
      "reason": "과도한 탈지를 줄여 민감하고 건조한 피부의 당김을 줄이는 데 도움이 됩니다.",
      "guidance": "미온수로 짧게 세안하고 충분히 헹구세요."
    },
    {
      "careArea": "SKIN_CARE",
      "productType": "장벽 보습 크림",
      "ingredient": "세라마이드",
      "reason": "피부 장벽과 수분 유지에 도움을 줄 수 있습니다.",
      "guidance": "세안 후 물기가 마르기 전에 소량을 고르게 바르세요."
    },
    {
      "careArea": "SKIN_CARE",
      "productType": "진정 세럼",
      "ingredient": "판테놀",
      "reason": "건조함으로 인한 불편감 완화와 보습에 도움을 줄 수 있습니다.",
      "guidance": "하루 한 번부터 시작해 피부 반응을 확인하세요."
    }
  ],
  "cautionIngredients": [
    {
      "careArea": "CLEANSING",
      "productType": "강한 세정력의 클렌저",
      "ingredient": "고함량 설페이트 계면활성제",
      "reason": "현재 피부에서는 세정 후 당김과 자극을 높일 수 있습니다.",
      "guidance": "사용 빈도를 줄이고 사용 후 건조함이나 따가움을 확인하세요."
    },
    {
      "careArea": "SKIN_CARE",
      "productType": "고함량 각질 관리 제품",
      "ingredient": "AHA/BHA",
      "reason": "민감한 상태에서 잦게 사용하면 붉어짐이 심해질 수 있습니다.",
      "guidance": "낮은 농도와 주 1회부터 시작하고 다른 각질 관리 성분과 동시에 사용하지 마세요."
    },
    {
      "careArea": "SKIN_CARE",
      "productType": "향이 강한 기초 제품",
      "ingredient": "향료",
      "reason": "향료에 민감한 경우 자극 반응이 나타날 수 있습니다.",
      "guidance": "향료가 무조건 해로운 것은 아니지만 민감 반응이 있었다면 무향 제품을 우선하세요."
    }
  ],
  "disclaimer": "이 내용은 일반적인 관리 정보이며 의료 진단이 아닙니다. 새 제품은 국소 부위에 패치 테스트한 뒤 사용하고, 심한 염증이나 알레르기 반응은 피부과 전문의와 상담하세요.",
  "createdAt": "2026-08-15T10:30:00"
}
```

`careArea` 값은 다음 두 가지다.

- `CLEANSING`: 세안 제품과 세정 성분
- `SKIN_CARE`: 기초 화장품과 피부 관리 성분

### 가장 최근 추천 조회

```bash
curl http://localhost:8080/user-care/me/recommendations/latest \
  -H "Authorization: Bearer $TOKEN"
```

아직 생성한 추천이 없으면 다음과 같이 `404 Not Found`를 반환한다.

```json
{
  "message": "아직 생성된 피부 관리 성분 추천이 없습니다."
}
```

프로필이 완성되지 않았으면 생성 API가 `400 Bad Request`를 반환한다.

```json
{
  "message": "피부 관리 추천 전에 사용자 프로필을 먼저 완성해주세요."
}
```

추천 결과는 `user_care_recommendations` 테이블에 저장된다. 프론트는
`recommendedIngredients`와 `cautionIngredients`를 각각 세 장의 카드로 표시하고,
`reason`과 `guidance`를 상세 설명으로 사용할 수 있다.

## 15. 종료

각 `bootRun` 터미널에서 `Ctrl+C`를 누른다. 포트 점유 여부는 아래처럼 확인할 수 있다.

```bash
lsof -nP -iTCP:8080 -iTCP:8081 -iTCP:8082 -iTCP:8083 -iTCP:8084 -iTCP:8085 -iTCP:8086 -iTCP:8087 -sTCP:LISTEN
```
