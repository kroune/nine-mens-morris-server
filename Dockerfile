FROM amazoncorretto:21-alpine
COPY ./build/libs/MensMorris-backend-all.jar /tmp/server.jar
WORKDIR /tmp
ENTRYPOINT ["java","-jar","server.jar"]