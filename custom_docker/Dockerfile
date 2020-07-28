FROM b2ihealthcare/snow-owl-oss:7.7.0

# change the config path
ENV SO_PATH_CONF="/opt/snowowl/bbl/config"

# define arguments accepted at build
ARG ELASTICSEARCH_CLUSTER_URL
ENV ELASTICSEARCH_CLUSTER_URL $ELASTICSEARCH_CLUSTER_URL
ARG ELASTICSEARCH_CONNECT_TIMEOUT
ENV ELASTICSEARCH_CONNECT_TIMEOUT $ELASTICSEARCH_CONNECT_TIMEOUT
ARG ELASTICSEARCH_SOCKET_TIMEOUT
ENV ELASTICSEARCH_SOCKET_TIMEOUT $ELASTICSEARCH_SOCKET_TIMEOUT

# prepare and copy over the snowowl.yml file
COPY --chown=snowowl:snowowl snowowl.yml /opt/snowowl/bbl/config

RUN echo "repository:" >> /opt/snowowl/bbl/config/snowowl.yml
RUN echo "  index:" >> /opt/snowowl/bbl/config/snowowl.yml
RUN echo "    clusterUrl: \${ELASTICSEARCH_CLUSTER_URL}" >> /opt/snowowl/bbl/config/snowowl.yml
RUN echo "    connectTimeout: \${ELASTICSEARCH_CONNECT_TIMEOUT}" >> /opt/snowowl/bbl/config/snowowl.yml
RUN echo "    socketTimeout: \${ELASTICSEARCH_SOCKET_TIMEOUT}" >> /opt/snowowl/bbl/config/snowowl.yml
