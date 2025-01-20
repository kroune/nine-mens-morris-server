import http from 'k6/http';

export const options = {
    stages: [
        {
            duration: '10s',
            target: 100
        },
        {
            duration: '5m',
            target: 100
        },
        {
            duration: '10s',
            target: 0
        }
    ]
};


export default function () {
    http.get('http://localhost:30066');
}