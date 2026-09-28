# 가상 결제대행사. 멀티 스테이지 빌드: 빌드 환경과 실행 환경을 분리해 최종 이미지를 가볍게 만든다.
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
COPY src src
RUN ./gradlew clean bootJar -x test --no-daemon --console=plain

FROM eclipse-temurin:17-jre-jammy
RUN useradd --create-home --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser
EXPOSE 9090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
