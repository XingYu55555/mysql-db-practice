FROM nginx:1.25
RUN apt-get update && apt-get install -y --no-install-recommends \
    nginx-module-njs \
    && rm -rf /var/lib/apt/lists/*
COPY nginx.conf /etc/nginx/nginx.conf
COPY auth.js /etc/nginx/auth.js
EXPOSE 8080
