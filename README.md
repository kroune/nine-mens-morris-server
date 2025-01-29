# Backend server for my "Nine men's morris" game
This project is a backend used in my game. You can [view](NineMensMorris_backend-openapi.yaml) api specification in openapi format. This format allows you to view it as using Swagger or Redoc
<br>
<br>
This project has 
* Rate limits
* Uses sql framework [exposed](https://github.com/JetBrains/Exposed) (safe from sql injections and other)
* Has a strong security
* easy to use api
* "searching for a game" queue, which finds an enemy in some rating range, using efficient bucket-based algorithm