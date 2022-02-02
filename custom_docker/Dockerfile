FROM b2ihealthcare/snow-owl-oss@sha256:4a50f060776f1a4f2e35d60cc8ac6284b00c42cd28e1b96991eb5ce8ba0a87ad

# change the config path
ENV SO_PATH_CONF="/opt/snowowl/bbl/config"

# Temporary work-around to disable Log4j exploit
# https://github.com/b2ihealthcare/snow-owl/issues/964#issuecomment-1027102086
# SO_JAVA_OPTS is used here: https://github.com/b2ihealthcare/snow-owl/blob/7.x/releng/com.b2international.snowowl.server.update/assembly/common/bin/snowowl.sh#L49-L73
ENV SO_JAVA_OPTS="-Dlog4j2.formatMsgNoLookups=true $SO_JAVA_OPTS"

COPY --chown=snowowl:snowowl snowowl.yml /opt/snowowl/bbl/config/
RUN mv /usr/share/snowowl/configuration/* $SO_PATH_CONF/


RUN rm -rf /usr/share/snowowl/configuration  && \
ln -s $SO_PATH_CONF /usr/share/snowowl/configuration

RUN ln -s $SO_PATH_CONF /etc/snowowl && \
    ln -s /usr/share/snowowl/resources /var/lib/snowowl && \
    ln -s /usr/share/snowowl/serviceability /var/log/snowowl

# define envs
ENV LOG_LEVEL $LOG_LEVEL
ENV ELASTICSEARCH_CLUSTER_URL $ELASTICSEARCH_CLUSTER_URL
ENV ELASTICSEARCH_CONNECT_TIMEOUT $ELASTICSEARCH_CONNECT_TIMEOUT
ENV ELASTICSEARCH_SOCKET_TIMEOUT $ELASTICSEARCH_SOCKET_TIMEOUT

# prepare and copy over the snowowl.yml file
COPY --chown=snowowl:snowowl snowowl.yml /opt/snowowl/bbl/config/

# prepare and copy over the serviceability.xml file
COPY --chown=snowowl:snowowl serviceability.xml /opt/snowowl/bbl/config/
