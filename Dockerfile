FROM openjdk:8u121-jre-alpine
MAINTAINER OpenTransport version: 0.1

RUN apk add --update curl bash ttf-dejavu && \
    rm -rf /var/cache/apk/*
VOLUME /opt/opentripplanner/graphs

ENV OTP_ROOT="/opt/opentripplanner"
ENV ROUTER_DATA_CONTAINER_URL="https://api.opentransport.ro/data/v1/routing"

WORKDIR ${OTP_ROOT}

ADD run.sh ${OTP_ROOT}/run.sh
ADD target/*-shaded.jar ${OTP_ROOT}/otp-shaded.jar

ENV PORT=8080
EXPOSE ${PORT}
ENV SECURE_PORT=8081
EXPOSE ${SECURE_PORT}
ENV ROUTER_NAME=romania
ENV JAVA_OPTS="-Xms4G -Xmx4G"

ENTRYPOINT exec ./run.sh
