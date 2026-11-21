# =============================================================================
# Stage 1: Build UI/forge-node (Spring Boot Java 21 application)
# =============================================================================
FROM eclipse-temurin:21-jdk-alpine AS ui-builder

WORKDIR /build/UI/forge-node

COPY UI/forge-node/mvnw ./
COPY UI/forge-node/.mvn ./.mvn
COPY UI/forge-node/pom.xml ./

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY UI/forge-node/src ./src
RUN ./mvnw clean package -DskipTests -B

# =============================================================================
# Stage 2: Forge Node Runtime Image (Alpine)
# =============================================================================
FROM alpine:3.20

# Set Java 21 environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-21-openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Mosquitto broker, Python 3, pip, OpenJRE 21, Nginx, OpenSSL, and Bash
RUN apk update && apk add --no-cache \
    mosquitto \
    mosquitto-clients \
    python3 \
    py3-pip \
    openjdk21-jre \
    nginx \
    openssl \
    bash \
    procps \
    && pip3 install --no-cache-dir --break-system-packages "paho-mqtt<2.0.0" \
    && rm -rf /var/cache/apk/*

# Set container working directory
WORKDIR /home/FORGE-NODE

# Copy required project directories into /home/FORGE-NODE (start with empty SHARED and AAU/script)
COPY AAU/ /home/FORGE-NODE/AAU/
COPY AU/ /home/FORGE-NODE/AU/
COPY heplers* helpers* /home/FORGE-NODE/helpers/
COPY UI/ /home/FORGE-NODE/UI/
RUN rm -rf /home/FORGE-NODE/AAU/script/* && mkdir -p /home/FORGE-NODE/SHARED /home/FORGE-NODE/AAU/script/themes /home/FORGE-NODE/AAU/script/full-configuration

# Copy compiled Spring Boot JAR from builder stage
COPY --from=ui-builder /build/UI/forge-node/target/*.jar /home/FORGE-NODE/UI/forge-node/target/forge-node.jar

# Ensure helper scripts are executable
RUN chmod +x /home/FORGE-NODE/helpers/*.sh 2>/dev/null || true

# Provide a lightweight systemctl wrapper so UI local Mosquitto controls work in Alpine
RUN cat <<'EOF' > /usr/local/bin/systemctl
#!/bin/bash
ACTION="$1"
SERVICE="$2"

if [ "$SERVICE" = "mosquitto" ] || [ "$SERVICE" = "mosquitto.service" ]; then
    case "$ACTION" in
        is-active)
            if pgrep -x mosquitto >/dev/null 2>&1; then
                echo "active"
                exit 0
            else
                echo "inactive"
                exit 3
            fi
            ;;
        enable|disable)
            exit 0
            ;;
        start)
            if ! pgrep -x mosquitto >/dev/null 2>&1; then
                /usr/sbin/mosquitto -c /etc/mosquitto/mosquitto.conf -d
            fi
            exit 0
            ;;
        stop)
            pkill -x mosquitto >/dev/null 2>&1 || true
            exit 0
            ;;
        restart)
            pkill -x mosquitto >/dev/null 2>&1 || true
            sleep 0.5
            /usr/sbin/mosquitto -c /etc/mosquitto/mosquitto.conf -d
            exit 0
            ;;
    esac
fi
exit 0
EOF
RUN chmod +x /usr/local/bin/systemctl

# Prepare Nginx runtime directories (no certificates baked into image)
RUN mkdir -p /etc/nginx/ssl /run/nginx

# Create startup entrypoint script to configure Nginx SSL mode and launch UI/forge-node
RUN cat <<'EOF' > /usr/local/bin/docker-entrypoint.sh
#!/bin/bash
set -e

cd /home/FORGE-NODE
mkdir -p /home/FORGE-NODE/AAU/script/themes /home/FORGE-NODE/AAU/script/full-configuration

SSL_MODE="$(echo "${FN_SSL_MODE:-none}" | tr '[:upper:]' '[:lower:]')"

if [ "$SSL_MODE" = "self-signed" ]; then
    openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
        -keyout /etc/nginx/ssl/forge-node.key \
        -out /etc/nginx/ssl/forge-node.crt \
        -subj "/C=RS/ST=Serbia/L=Kragujevac/O=ForgeNode/OU=UI/CN=localhost"
    chmod 600 /etc/nginx/ssl/forge-node.key
    chmod 644 /etc/nginx/ssl/forge-node.crt

    SSL_LISTEN="ssl"
    SSL_DIRECTIVES=$(cat <<'SSL'
    ssl_certificate     /etc/nginx/ssl/forge-node.crt;
    ssl_certificate_key /etc/nginx/ssl/forge-node.key;
    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;
    error_page 497 https://$host:10011$request_uri;
SSL
)
    FORWARDED_PROTO="https"
elif [ "$SSL_MODE" = "none" ]; then
    SSL_LISTEN=""
    SSL_DIRECTIVES=""
    FORWARDED_PROTO="\$scheme"
else
    echo "[Forge Node] ERROR: Invalid FN_SSL_MODE='${FN_SSL_MODE}'. Expected: none or self-signed" >&2
    exit 1
fi

cat <<NGINX > /etc/nginx/http.d/default.conf
server {
    listen 10011 ${SSL_LISTEN};
    listen [::]:10011 ${SSL_LISTEN};
    server_name _;

${SSL_DIRECTIVES}
    client_max_body_size 20M;

    location / {
        proxy_pass         http://127.0.0.1:10001;
        proxy_http_version 1.1;

        proxy_set_header   Host \$host:\$server_port;
        proxy_set_header   X-Real-IP \$remote_addr;
        proxy_set_header   X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header   X-Forwarded-Proto ${FORWARDED_PROTO};
        proxy_set_header   Connection "";

        # Support Server-Sent Events (SSE) for MQTT Real-Time Monitor
        proxy_buffering    off;
        proxy_cache        off;
        proxy_read_timeout 86400s;
        proxy_send_timeout 86400s;
    }
}
NGINX

# Start Nginx reverse proxy
nginx

# Start UI/forge-node Spring Boot application
exec java -jar /home/FORGE-NODE/UI/forge-node/target/forge-node.jar \
    --spring.config.additional-location=optional:file:/home/FORGE-NODE/UI/forge-node/src/main/resources/application.properties
EOF
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Expose Nginx port (10011)

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
