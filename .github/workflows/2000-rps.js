import http from 'k6/http';

export let options = {
    stages: [
        {
            vus: 200,
            target: 200,
            duration: '20s',
        },
        {
            duration: '50s',
            target: 200,
            vus: 200,
        },
        {
            duration: '10s',
            target: 0,
            vus: 0,
        },
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'], // http errors should be less than 1%
        http_req_duration: ['p(99)<500'],
    },
};


export default function () {
    http.get('http://localhost:30066');
}