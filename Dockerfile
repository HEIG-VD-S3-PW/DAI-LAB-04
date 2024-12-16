# # Base im,age
FROM eclipse-temurin:21-jdk-alpine AS clean-builder
FROM clean-builder AS builder

WORKDIR /app

# Download build dependencies
COPY mvnw pom.xml dependency-reduced-pom.xml ./
COPY .mvn ./.mvn
RUN chmod +x ./mvnw && ./mvnw install

# Copy project and build
COPY src ./src
RUN ./mvnw package

FROM clean-builder AS dev

ARG WATCHEXEC_VERSION="2.2.0"

WORKDIR /tmp

# Install watchexec and required dependencies
RUN apk update && \
  apk add curl xz && \
  curl -LO https://github.com/watchexec/watchexec/releases/download/v${WATCHEXEC_VERSION}/watchexec-${WATCHEXEC_VERSION}-x86_64-unknown-linux-musl.tar.xz && \
  tar -xf watchexec-${WATCHEXEC_VERSION}-x86_64-unknown-linux-musl.tar.xz && \
  mv watchexec-${WATCHEXEC_VERSION}-x86_64-unknown-linux-musl/watchexec /usr/bin && \
  rm watchexec-${WATCHEXEC_VERSION}-x86_64-unknown-linux-musl.tar.xz

WORKDIR /app

EXPOSE 7070 

COPY ./entrypoint.sh /app
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]

FROM eclipse-temurin:21-jre-alpine AS prod

WORKDIR /app

COPY --from=builder /app/target/app.jar /app/app.jar
# COPY ./target/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
