FROM openjdk:17-jdk-alpine

ENV LAS2PEER_PORT=9011

RUN apk add --update bash mysql-client curl && rm -f /var/cache/apk/*
RUN addgroup -g 1000 -S las2peer && \
    adduser -u 1000 -S las2peer -G las2peer

COPY --chown=las2peer:las2peer . /src
WORKDIR /src

# run the rest as unprivileged user
USER las2peer
RUN chmod +x gradlew && ./gradlew build --exclude-task test

EXPOSE $LAS2PEER_PORT
ENTRYPOINT ["/src/docker-entrypoint.sh"]
