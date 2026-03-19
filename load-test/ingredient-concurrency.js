/**
 * 재료 추가 동시 요청 중복 방지 부하테스트
 *
 * 시나리오 1 — sameKeyword:   동일 키워드 N개 동시 요청 → row 1개만 생성되는지 확인
 * 시나리오 2 — synonymKeyword: 동의어 키워드 동시 요청 → AI 정규화 후 동일 row로 수렴하는지 확인
 *   예) "브로콜리" + "broccoli" → 둘 다 정규명 "브로콜리"로 정규화되어 1개 row만 생성
 *
 * 실행 전:
 *   1. 서버 실행: docker compose up -d  또는  ./gradlew bootRun
 *   2. 아래 KEYWORD_A / KEYWORD_B 가 DB에 없는 상태여야 의미 있는 테스트 가능
 *      (이미 있으면 step1 캐시히트로 즉시 반환 — 에러는 아니지만 dedup 경로를 타지 않음)
 *
 * 실행:
 *   k6 run load-test/ingredient-concurrency.js
 *
 * 환경변수 오버라이드:
 *   k6 run --env BASE_URL=http://localhost:8080 \
 *           --env KEYWORD_A=브로콜리 \
 *           --env KEYWORD_B=broccoli \
 *           --env VUS=30 \
 *           load-test/ingredient-concurrency.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

// ── 설정 ────────────────────────────────────────────────────────────────────

const BASE_URL  = __ENV.BASE_URL  || 'http://localhost:8080';
const KEYWORD_A = __ENV.KEYWORD_A || '브로콜리';    // 시나리오 1·2 공통 기준 키워드
const KEYWORD_B = __ENV.KEYWORD_B || 'broccoli';   // 시나리오 2 동의어 (KEYWORD_A와 같은 정규명으로 수렴해야 함)
const VUS       = parseInt(__ENV.VUS || '20');

// ── 커스텀 메트릭 ────────────────────────────────────────────────────────────

const successRate  = new Rate('generate_success');
const duplicateRow = new Counter('duplicate_rows_detected'); // teardown에서 중복 발견 시 증가

// ── 시나리오 정의 ─────────────────────────────────────────────────────────────
//   시나리오 2는 시나리오 1 종료 + AI 생성 여유시간(90s) 이후 시작

export const options = {
  scenarios: {
    // 시나리오 1: 동일 키워드 동시 요청
    sameKeyword: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: VUS,
      maxDuration: '60s',
      exec: 'testSameKeyword',
      startTime: '0s',
    },
    // 시나리오 2: 동의어 키워드 동시 요청
    //   VUS의 절반은 KEYWORD_A, 나머지 절반은 KEYWORD_B 사용
    synonymKeyword: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: VUS,
      maxDuration: '60s',
      exec: 'testSynonymKeyword',
      startTime: '90s',
    },
  },
  thresholds: {
    generate_success:      ['rate==1.0'],  // 모든 요청 2xx
    duplicate_rows_detected: ['count==0'], // 중복 row 없음
  },
};

// ── 시나리오 1: 동일 키워드 ─────────────────────────────────────────────────

export function testSameKeyword() {
  const res = generate(KEYWORD_A);
  const ok = check(res, {
    '[sameKeyword] status 200 or 202': (r) => r.status === 200 || r.status === 202,
    '[sameKeyword] body has data.id':   (r) => parseId(r) !== null,
  });
  successRate.add(ok);
  logResponse('sameKeyword', KEYWORD_A, res);
}

// ── 시나리오 2: 동의어 키워드 ───────────────────────────────────────────────

export function testSynonymKeyword() {
  // 짝수 VU → KEYWORD_A, 홀수 VU → KEYWORD_B (동의어)
  const keyword = __VU % 2 === 0 ? KEYWORD_A : KEYWORD_B;

  const res = generate(keyword);
  const ok = check(res, {
    '[synonymKeyword] status 200 or 202': (r) => r.status === 200 || r.status === 202,
    '[synonymKeyword] body has data.id':   (r) => parseId(r) !== null,
  });
  successRate.add(ok);
  logResponse('synonymKeyword', keyword, res);
}

// ── 최종 검증 (두 시나리오 모두 완료 후 1회) ─────────────────────────────────

export function teardown() {
  console.log('\n======================================================');
  console.log('  최종 검증');
  console.log('======================================================');

  // generate로 현재 id를 얻고 status 엔드포인트로 폴링
  // (search는 COMPLETED만 반환하므로 PENDING/FAILED 상태를 감지할 수 없음)
  const idA = getIngredientId(KEYWORD_A);
  if (!idA) {
    console.error(`❌ "${KEYWORD_A}" id 조회 실패`);
    duplicateRow.add(1);
    return;
  }

  const completedA = waitForCompletedById(idA, 90);
  if (!completedA) {
    console.error(`❌ 90초 내에 id=${idA} COMPLETED 미도달 (서버 로그에서 AI 생성 오류 확인)`);
    duplicateRow.add(1);
    return;
  }

  console.log(`✅ [시나리오 1] "${KEYWORD_A}" → id=${idA} COMPLETED`);

  // KEYWORD_B가 동일한 id로 수렴했는지 확인
  const idB = getIngredientId(KEYWORD_B);
  if (!idB) {
    console.error(`❌ [시나리오 2] "${KEYWORD_B}" id 조회 실패`);
    duplicateRow.add(1);
  } else if (idB !== idA) {
    console.error(`❌ [시나리오 2] 동의어가 다른 row로 저장됨: "${KEYWORD_A}"=id${idA}, "${KEYWORD_B}"=id${idB}`);
    duplicateRow.add(1);
  } else {
    console.log(`✅ [시나리오 2] "${KEYWORD_B}" → 동일 row (id=${idB}) 로 수렴`);
  }

  console.log('\n서버 로그에서 AI 호출 횟수도 확인하세요:');
  console.log('  grep "AI 재료 생성 시작" <server-log>');
  console.log(`  → "${KEYWORD_A}" 가 정확히 1번만 나와야 합니다`);
  console.log('======================================================\n');
}

// ── 헬퍼 ─────────────────────────────────────────────────────────────────────

function generate(keyword) {
  return http.post(
    `${BASE_URL}/api/v1/ingredients/generate`,
    JSON.stringify({ keyword }),
    { headers: { 'Content-Type': 'application/json' }, timeout: '30s' },
  );
}

/** generate 엔드포인트로 keyword의 현재 ingredient id를 반환 (PENDING/FAILED/COMPLETED 무관) */
function getIngredientId(keyword) {
  const res = generate(keyword);
  if (res.status !== 200 && res.status !== 202) return null;
  try { return JSON.parse(res.body).data?.id ?? null; } catch { return null; }
}

/** status 엔드포인트로 id가 COMPLETED가 될 때까지 폴링 */
function waitForCompletedById(id, timeoutSec) {
  const deadline = Date.now() + timeoutSec * 1000;
  while (Date.now() < deadline) {
    const res = http.get(`${BASE_URL}/api/v1/ingredients/${id}/status`);
    if (res.status === 200) {
      const data = JSON.parse(res.body).data;
      if (data?.status === 'COMPLETED') return data;
      console.log(`  폴링 중... id=${id} status=${data?.status}`);
    }
    sleep(3);
  }
  return null;
}

function parseId(res) {
  try { return JSON.parse(res.body).data?.id ?? null; } catch { return null; }
}

function logResponse(scenario, keyword, res) {
  if (res.status === 200 || res.status === 202) {
    const body = JSON.parse(res.body);
    console.log(`[${scenario}] VU ${__VU}: HTTP ${res.status} | keyword="${keyword}" | id=${body.data?.id} | status=${body.data?.status}`);
  } else {
    console.error(`[${scenario}] VU ${__VU}: 실패 HTTP ${res.status} | keyword="${keyword}" | body=${res.body}`);
  }
}
