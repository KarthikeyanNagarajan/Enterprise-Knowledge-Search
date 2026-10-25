FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy parent pom and all module poms first so Maven can cache dependency resolution.
COPY pom.xml ./
COPY eks-common/pom.xml ./eks-common/pom.xml
COPY eks-identity/pom.xml ./eks-identity/pom.xml
COPY eks-document/pom.xml ./eks-document/pom.xml
COPY eks-rag/pom.xml ./eks-rag/pom.xml
COPY eks-chat/pom.xml ./eks-chat/pom.xml
COPY eks-agent/pom.xml ./eks-agent/pom.xml
COPY eks-app/pom.xml ./eks-app/pom.xml

RUN mvn -q -B -F pom.xml dependency:go-offline || true

# Copy all source trees after dependency prefetch for efficient incremental builds.
COPY eks-common ./eks-common
COPY eks-identity ./eks-identity
COPY eks-document ./eks-document
COPY eks-rag ./eks-rag
COPY eks-chat ./eks-chat
COPY eks-agent ./eks-agent
COPY eks-app ./eks-app

RUN mvn -q -B clean package -DskipTests -pl eks-app -am

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S eks && adduser -S -G eks eks

COPY --from=build /workspace/eks-app/target/eks-app-*.jar /app/app.jar

USER eks
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
