# Backend server for my "Nine men's morris" game
This project is a backend used in my game. You can [view](https://github.com/kroune/nine-mens-morris-server/blob/v0.0.6/NineMensMorris_backend-openapi.yaml) api specification in openapi format. This format allows you to view it as using Swagger or Redoc
<br>
<br>
## This project has
* account system
* user info (for example profile picture)
* Rate limits
* Uses sql framework [exposed](https://github.com/JetBrains/Exposed) (safe from sql injections and other)
* Has a strong security (I hope so)
* easy to use api
* "searching for a game" queue, which finds an enemy in some rating range, using efficient bucket-based algorithm

Running a benchmark using k6 on an 8 GB & 2 cpu instance host was able to handle up to 4 000 rps (simple ones) with reasonable latency

This project has a testing workflow & has workflow that deploys current version of the backend to docker hub

## Demo
Since this is a backend, it is hard to make a good demo, but you can test in using postman/curl/[scalar](https://nine-men-s-morris.apidocumentation.com/reference) (or any other similar app) or test how it servers our [app](https://github.com/kroune/nine-mens-morris-app-kmp)