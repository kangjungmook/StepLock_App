#!/usr/bin/env bash
# 빌드 로그를 이슈로 남깁니다. Actions 실행 로그는 blob 호스트로 리다이렉트돼
# 외부에서 받기 어려워서, 오류 줄과 로그 끝부분을 이슈 본문에 담습니다.
set -euo pipefail

TITLE="${1:-빌드 실패}"
LOG="${2:-build.log}"

if [ ! -f "$LOG" ]; then
  echo "로그 파일이 없습니다: $LOG" >&2
  exit 0
fi

{
  echo "커밋 \`${GITHUB_SHA}\` · [실행 로그](${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID})"
  echo
  echo '### 오류로 보이는 줄'
  echo '```'
  grep -nE "^e: |error:|^FAILURE:|^Caused by:|Unresolved reference|FAILED$|^\* What went wrong|^\* Where|Missing class|R8: " "$LOG" \
    | head -80 || echo '(패턴에 걸린 줄이 없습니다 — 아래 전문을 보세요)'
  echo '```'
  echo
  echo '### 로그 마지막 150줄'
  echo '```'
  tail -n 150 "$LOG"
  echo '```'
} > issue.md

gh issue create --title "${TITLE} · ${GITHUB_SHA}" --body-file issue.md
