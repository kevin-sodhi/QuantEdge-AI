FROM node:20-slim AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .

# Declare build args so Railway can pass VITE_* vars into the build stage
ARG VITE_PYTHON_URL
ARG VITE_NODE_URL
ENV VITE_PYTHON_URL=$VITE_PYTHON_URL
ENV VITE_NODE_URL=$VITE_NODE_URL

RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
