FROM   openjdk:8
#FROM  java:8
LABEL authors="Brugnara <martin.brugnara@gmail.com>, Matteo Lissandrini <ml@disi.unitn.eu>, Nolan Nichols <nolan.nichols@gmail.com>"


ENV GREMLIN3_TAG 3.6.5

ENV GREMLIN3_HOME /opt/gremlin

ENV PATH /opt/gremlin/bin:$PATH


RUN apt-get -q  update && \
    apt-get -q  upgrade -y && \
    apt-get -q  install -y --no-install-recommends \
        build-essential \
        libstdc++6 \
        libgoogle-perftools4 \
        ca-certificates \
        pwgen \
        openssl \
        curl \
        bash \
        maven \
        unzip \
        git-core \
        openjfx \
        nano

RUN curl -L -o /tmp/gremlin.zip \
    http://mirror.nohup.it/apache/tinkerpop/${GREMLIN3_TAG}/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG}-bin.zip && \
    unzip -q /tmp/gremlin.zip -d /opt/ && \
    rm /tmp/gremlin.zip && \
    ln -s /opt/apache-tinkerpop-gremlin-console-${GREMLIN3_TAG} ${GREMLIN3_HOME}

COPY extra/stackoverflow_1.64m.json /tmp/stackoverflow_1.64m.json

RUN mv /tmp/stackoverflow_1.64m.json /opt/stackoverflow_1.64m.json

RUN mkdir /tmp/import

COPY extra/import/* /tmp/import/

RUN mv /tmp/import /opt/

RUN ls /opt/import/

WORKDIR /opt/

COPY extra/safe.sh /tmp/safe.sh

COPY extra/.groovy /root/.groovy
COPY extra/activate-sugar-tp3.groovy /tmp/

RUN  ${GREMLIN3_HOME}/bin/gremlin.sh -e /tmp/activate-sugar-tp3.groovy


WORKDIR /runtime

CMD ["gremlin.sh", "-e", "/runtime/stackoverflow.groovy"]

#CMD ["bash"]
