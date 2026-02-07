FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY . .

RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

FROM amazoncorretto:17
WORKDIR /

# Gradle 실행에 필요한 유틸리티 설치 (리눅스 환경 필수)
RUN yum update -y && yum install -y findutils && yum clean all

COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
