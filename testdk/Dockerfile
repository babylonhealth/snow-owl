FROM b2ihealthcare/snow-owl-oss:7.x

# set the user and password
ARG SNOWOWL_USERNAME
ARG SNOWOWL_PASSWORD

RUN echo " pass ${SNOWOWL_PASSWORD} "
RUN echo "${SNOWOWL_USERNAME}:${SNOWOWL_PASSWORD}" >> /usr/share/snowowl/configuration/users

# prepare and copy over the snowowl.yml file
COPY --chown=snowowl:snowowl snowowl.yml /usr/share/snowowl/configuration/

ARG ELASTICSEARCH_CLUSTER_URL
ARG ELASTICSEARCH_CONNECT_TIMEOUT
ARG ELASTICSEARCH_SOCKET_TIMEOUT

RUN echo "repository:" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "  index:" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "    clusterUrl: ${ELASTICSEARCH_CLUSTER_URL}" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "    connectTimeout: ${ELASTICSEARCH_CONNECT_TIMEOUT}" >>  /usr/share/snowowl/configuration/snowowl.yml
RUN echo "    socketTimeout: ${ELASTICSEARCH_SOCKET_TIMEOUT}" >>  /usr/share/snowowl/configuration/snowowl.yml

EXPOSE 2036 8080