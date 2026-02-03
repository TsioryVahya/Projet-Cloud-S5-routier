# Étape 1 : Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Étape 2 : Serveur Nginx
FROM nginx:alpine
# Copier la configuration Nginx personnalisée
COPY nginx.conf /etc/nginx/conf.d/default.conf
# Copier les fichiers buildés
COPY --from=build /app/dist/web-app /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
