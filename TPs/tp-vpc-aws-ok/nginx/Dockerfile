FROM nginx:1.27-alpine

# Supprimer la config par défaut
RUN rm /etc/nginx/conf.d/default.conf

# Copier notre config
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD wget -qO- http://localhost/nginx-health || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
