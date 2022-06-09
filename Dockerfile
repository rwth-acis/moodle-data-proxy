FROM openjdk:17-jdk-alpine

ENV LAS2PEER_PORT=9011

RUN apk add --update bash mysql-client curl dos2unix && rm -f /var/cache/apk/*
RUN addgroup -g 1000 -S las2peer && \
    adduser -u 1000 -S las2peer -G las2peer

COPY --chown=las2peer:las2peer . /src
WORKDIR /src

# run the rest as unprivileged user
USER las2peer
RUN dos2unix gradlew
RUN dos2unix gradle.properties
RUN dos2unix /src/docker-entrypoint.sh
RUN dos2unix /src/etc/i5.las2peer.webConnector.WebConnector.properties
RUN dos2unix /src/etc/i5.las2peer.services.moodleDataProxyService.MoodleDataProxyService.properties
RUN dos2unix /src/etc/i5.las2peer.registry.data.RegistryConfiguration.properties
RUN chmod +x gradlew && ./gradlew build --exclude-task test

EXPOSE $LAS2PEER_PORT
ENTRYPOINT ["/src/docker-entrypoint.sh"]
