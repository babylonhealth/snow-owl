FROM b2ihealthcare/snow-owl-oss:7.7.0

# define envs
ARG ELASTICSEARCH_CLUSTER_URL
ENV ELASTICSEARCH_CLUSTER_URL $ELASTICSEARCH_CLUSTER_URL
ARG ELASTICSEARCH_CONNECT_TIMEOUT
ENV ELASTICSEARCH_CONNECT_TIMEOUT $ELASTICSEARCH_CONNECT_TIMEOUT
ARG ELASTICSEARCH_SOCKET_TIMEOUT
ENV ELASTICSEARCH_SOCKET_TIMEOUT $ELASTICSEARCH_SOCKET_TIMEOUT

# prepare and copy over the snowowl.yml file
COPY --chown=snowowl:snowowl snowowl.yml /opt/snowowl/bbl/config/

# change the config path
ENV SO_PATH_CONF="/opt/snowowl/bbl/config"
RUN mv /usr/share/snowowl/configuration/* $SO_PATH_CONF/
