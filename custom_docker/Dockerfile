FROM b2ihealthcare/snow-owl-oss:7.7.0

# define arguments accepted at build
ARG SNOWOWL_USERNAME
ARG SNOWOWL_PASSWORD

ARG ELASTICSEARCH_CLUSTER_URL
ARG ELASTICSEARCH_CONNECT_TIMEOUT
ARG ELASTICSEARCH_SOCKET_TIMEOUT

# set the user and password
RUN echo "${SNOWOWL_USERNAME}:${SNOWOWL_PASSWORD}" > /usr/share/snowowl/configuration/users

# prepare and copy over the snowowl.yml file
COPY --chown=snowowl:snowowl snowowl.yml /usr/share/snowowl/configuration/

RUN echo "repository:" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "  index:" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "    clusterUrl: ${ELASTICSEARCH_CLUSTER_URL}" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "    connectTimeout: ${ELASTICSEARCH_CONNECT_TIMEOUT}" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "    socketTimeout: ${ELASTICSEARCH_SOCKET_TIMEOUT}" >>  /usr/share/snowowl/configuration/snowowl.yml