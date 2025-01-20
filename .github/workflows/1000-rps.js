import http from 'k6/http';

export const options = {
    stages: [
        {
            duration: '30s',
            target: 1000
        },
        {
            duration: '5m',
            target: 1000
        },
        {
            duration: '30s',
            target: 0
        }
    ]
};


export default function () {
    const response = http.get('http://localhost:30066');
}