FROM nginx:1.25.5-alpine

COPY src /usr/share/nginx/src

CMD ["nginx", "-g", "daemon off;"]