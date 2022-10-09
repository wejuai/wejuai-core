FROM openjdk:11-jre-slim
COPY libs/lib /app/lib
COPY libs/*.jar /app/app.jar
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar","--spring.resources.staticLocations[0]=file:/static/"]