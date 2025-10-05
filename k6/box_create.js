import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

export let createErrorRate = new Rate('create_errors');
export let createSuccess = new Counter('create_success_count');
export let createLatency = new Trend('create_latency_ms');

export let options = {
    scenarios: {
        ramp_rps: {
            executor: 'ramping-arrival-rate',
            startRate: 1,          // 시작 RPS
            timeUnit: '1s',
            preAllocatedVUs: 100,  // 시작시 미리 할당할 VU 풀 (초기 ramp 안정성)
            maxVUs: 300,           // VU 상한 (여기서 부족하면 dropped_iterations 발생)
            stages: [
                { target: 10, duration: '30s' },   // 10 rps
                { target: 50, duration: '1m' },    // ramp to 50 rps
                { target: 100, duration: '2m' },   // ramp to 100 rps
                { target: 0, duration: '30s' },    // ramp down
            ],
            gracefulStop: '30s'     // (선택) 종료 시 진행중인 요청이 정리될 시간
        }
    },
    thresholds: {
        // 성공 응답 비율/에러율과 지연을 설정
        'create_errors': ['rate<0.02'],                // 에러율 < 2%
        'create_latency_ms': ['p(95)<800'],            // 생성 p95 < 800ms
        'http_req_failed': ['rate<0.05']               // 전체 실패율 < 5%
    }
};

// env
const BASE = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const ADMIN_TOKEN = __ENV.ADMIN_TOKEN || ''; // 필수
const START_INDEX = Number(__ENV.START_INDEX || '0');

if (!ADMIN_TOKEN) {
    throw new Error('ADMIN_TOKEN required for create tests. Set ADMIN_TOKEN env var.');
}

function makePayload(idx) {
    // 고유한 코드 생성: VU/ITER/시간 결합
    // __VU, __ITER are available globally in k6 runtime
    const unique = `loadtest-${__VU}-${__ITER}-${Date.now()}-${idx}`;
    return {
        boxCode: unique,
        boxName: `Loadtest box ${unique}`,
        // 필요시 추가 필드 삽입
    };
}

export default function () {
    const idx = START_INDEX;
    const payload = makePayload(idx);
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': `Bearer ${ADMIN_TOKEN}`
    };
    const url = `${BASE}/api/v1/boxes`;

    const start = Date.now();
    const res = http.post(url, JSON.stringify(payload), { headers: headers, timeout: '30s' });
    const latency = Date.now() - start;
    createLatency.add(latency);

    // 허용되는 응답: 201(created) 또는 409(conflict/already exists)
    const ok = check(res, {
        'status is 201 or 409': r => r.status === 201 || r.status === 409
    });

    if (res.status === 201) {
        createSuccess.add(1);
    }

    if (!ok) {
        createErrorRate.add(1);
        // 로그는 과다 출력 주의 — 필요시 주석
        console.error(`create failed status=${res.status} body=${res.body}`);
    }

    // 작은 think time (arrival-rate가 요청 간격을 제어하므로 최소 sleep)
    sleep(0.05);
}
