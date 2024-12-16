# # Base im,age
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /tmp

# Install watchexec and required dependencies
RUN apt-get update && \
  apt-get install -y curl xz-utils && \
  curl -LO https://github.com/watchexec/watchexec/releases/download/v2.2.0/watchexec-2.2.0-x86_64-unknown-linux-gnu.tar.xz && \
  tar -xf watchexec-2.2.0-x86_64-unknown-linux-gnu.tar.xz && \
  mv  watchexec-2.2.0-x86_64-unknown-linux-gnu/watchexec /usr/bin && \
  rm watchexec-2.2.0-x86_64-unknown-linux-gnu.tar.xz

WORKDIR /app

EXPOSE 7070 

COPY ./entrypoint.sh /app
RUN chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
