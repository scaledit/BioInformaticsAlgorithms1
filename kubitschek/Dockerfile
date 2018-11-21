FROM java:8
MAINTAINER Ashley Penney, apenney@ntoggle.com

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get -yq install numactl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

RUN mkdir /logs && chown 1:1 -R /logs

WORKDIR /

USER daemon

EXPOSE 9000

ADD target/scala-2.11/kubitschek.jar /app/kubitschek.jar

VOLUME /logs

CMD [ "java", "-jar", "/app/kubitschek.jar" ]
