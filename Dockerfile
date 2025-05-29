# Dockerfile para nginx personalizado
FROM nginx:latest

COPY nginx.conf /etc/nginx/nginx.conf
