FROM amazoncorretto:17
COPY ./build/libs/MensMorris-backend-all.jar /tmp/server.jar
WORKDIR /tmp
ENTRYPOINT ["java","-jar","server.jar"]