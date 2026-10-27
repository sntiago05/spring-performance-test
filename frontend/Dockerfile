# Multi-stage Dockerfile for HelpFlow Web
FROM node:24-alpine AS build

WORKDIR /app

# Install dependencies using package-lock if present or package.json
COPY package*.json ./
RUN npm ci

# Copy source and compile production build
COPY . .
RUN npm run build -- --configuration production

# Production web server
FROM nginx:1.27-alpine

# Replace default configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/security-headers.conf

# Copy compiled frontend assets from build stage
COPY --from=build /app/dist/helpflow-web/browser /usr/share/nginx/html

# Permissions setup
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chmod -R 755 /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
