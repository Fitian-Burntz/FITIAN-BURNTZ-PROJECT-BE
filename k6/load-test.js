import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// 커스텀 메트릭 정의
const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const successfulRequests = new Counter('successful_requests');

// 테스트 옵션 설정 - TPS 기반
export const options = {
    scenarios: {
        // 시나리오 1: 낮은 부하 (100 TPS)
        low_load: {
            executor: 'constant-arrival-rate',
            rate: 100,              // 초당 100 요청
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 50,
            maxVUs: 100,
            startTime: '0s',
            tags: { test_type: 'low_load' },
        },

        // 시나리오 2: 중간 부하 (300 TPS)
        medium_load: {
            executor: 'constant-arrival-rate',
            rate: 300,              // 초당 300 요청
            timeUnit: '1s',
            duration: '3m',
            preAllocatedVUs: 100,
            maxVUs: 200,
            startTime: '2m',
            tags: { test_type: 'medium_load' },
        },

        // 시나리오 3: 높은 부하 (500 TPS)
        high_load: {
            executor: 'constant-arrival-rate',
            rate: 500,              // 초당 500 요청
            timeUnit: '1s',
            duration: '3m',
            preAllocatedVUs: 200,
            maxVUs: 300,
            startTime: '5m',
            tags: { test_type: 'high_load' },
        },

        // 시나리오 4: 최대 부하 (1000 TPS) - 선택적
        spike_load: {
            executor: 'constant-arrival-rate',
            rate: 1000,             // 초당 1000 요청
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 300,
            maxVUs: 500,
            startTime: '8m',
            tags: { test_type: 'spike_load' },
        },
    },

    // 성능 임계값 설정
    thresholds: {
        'http_req_duration': ['p(95)<500', 'p(99)<1000'], // 95%는 500ms 이하, 99%는 1000ms 이하
        'http_req_failed': ['rate<0.01'],  // 에러율 1% 미만
        'http_reqs': ['rate>50'],          // 최소 50 TPS 이상
        'errors': ['rate<0.05'],           // 커스텀 에러율 5% 미만
    },
};

// 테스트 설정
const BASE_URL = 'http://localhost:8080';
const API_ENDPOINT = '/api/v1/boxes/1/wods/2025-10-30/records';

export default function () {
    // API 호출
    const url = `${BASE_URL}${API_ENDPOINT}`;

    const params = {
        headers: {
            'Content-Type': 'application/json',
            // 필요한 경우 인증 헤더 추가
            // 'Authorization': 'Bearer YOUR_TOKEN',
        },
        tags: {
            name: 'GetWODRecords',
        },
    };

    const response = http.get(url, params);

    // 응답 검증
    const checkResult = check(response, {
        '상태 코드 200': (r) => r.status === 200,
        '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        '응답 시간 < 1000ms': (r) => r.timings.duration < 1000,
        '응답 본문 존재': (r) => r.body && r.body.length > 0,
        '올바른 Content-Type': (r) => r.headers['Content-Type']?.includes('application/json'),
    });

    // 커스텀 메트릭 기록
    errorRate.add(!checkResult);
    responseTime.add(response.timings.duration);

    if (response.status === 200) {
        successfulRequests.add(1);
    }

    // 에러 로깅
    if (response.status !== 200) {
        console.error(`Error: Status ${response.status}, Body: ${response.body}`);
    }
}

// 테스트 시작 시 실행
export function setup() {
    console.log('=== K6 TPS 기반 부하 테스트 시작 ===');
    console.log(`Target URL: ${BASE_URL}${API_ENDPOINT}`);
    console.log('');
    console.log('테스트 시나리오:');
    console.log('  0-2분: 100 TPS (낮은 부하)');
    console.log('  2-5분: 300 TPS (중간 부하)');
    console.log('  5-8분: 500 TPS (높은 부하)');
    console.log('  8-10분: 1000 TPS (스파이크 부하)');
    console.log('');

    // API 상태 확인
    const response = http.get(`${BASE_URL}${API_ENDPOINT}`);
    if (response.status !== 200) {
        console.error('경고: API가 정상 응답하지 않습니다.');
    } else {
        console.log('✓ API 상태 정상');
    }

    return { startTime: new Date() };
}

// 테스트 종료 시 실행
export function teardown(data) {
    const endTime = new Date();
    const duration = (endTime - data.startTime) / 1000;
    console.log('');
    console.log('=== K6 부하 테스트 완료 ===');
    console.log(`총 테스트 시간: ${duration.toFixed(2)}초`);
    console.log('');
    console.log('💡 결과 분석 팁:');
    console.log('  - http_reqs: 총 요청 수 및 평균 TPS');
    console.log('  - http_req_duration: 응답 시간 (avg, p95, p99)');
    console.log('  - http_req_failed: 실패율');
}