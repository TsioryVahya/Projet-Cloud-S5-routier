# Étape 1 : Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# On copie le pom.xml d'abord pour mettre en cache les dépendances
COPY pom.xml .

# On télécharge les dépendances
RUN mvn dependency:go-offline -B

# On copie le code source
COPY src ./src

# On build le projet
RUN mvn package -DskipTests -B

# Étape 2 : Run (Image finale légère)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.war app.war
COPY src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json
EXPOSE 8081
ENTRYPOINT ["java","-jar","app.war"]
