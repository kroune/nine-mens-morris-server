import http from 'k6/http';

export let options = {
    stages: [
        {
            vus: 200,
            target: 200,
            duration: '20s',
        },
        {
            duration: '20s',
            target: 200,
            vus: 200,
        },
        {
            duration: '2m',
            target: 500,
            vus: 500,
        },
        {
            duration: '4m',
            target: 1000,
            vus: 1000,
        },
        {
            duration: '6m',
            target: 1500,
            vus: 1500,
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