# ==========================================
# ETAPA 1: BUILD (Construção do Projeto)
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# 1. OTIMIZAÇÃO DE CACHE: Copia apenas o pom.xml primeiro.
# Isso faz o Docker baixar as dependências uma vez só e não repetir o processo
# a menos que você instale uma biblioteca nova. Deixa o deploy no Render muito mais rápido!
COPY pom.xml .
RUN mvn dependency:go-offline

# 2. Copia o código fonte e gera o executável
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: RUNTIME (Servidor de Produção)
# ==========================================
# 3. IMAGEM MAIS LEVE: Usamos a versão "jre" (Java Runtime Environment) em vez da "jdk".
# O JRE só tem o que é necessário para RODAR o app, o que deixa o servidor mais rápido.
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# 4. Copia o .jar gerado na Etapa 1
COPY --from=build /app/target/*.jar app.jar

# 5. Expõe a porta 8080 (O Render lida com isso automaticamente)
EXPOSE 8080

# 6. A DIETA DO JAVA: Forçamos o limite de memória direto no coração do Docker
# Isso impede o "OOM Killed" (Morte por falta de memória) no plano gratuito.
ENV JAVA_OPTS="-Xms256m -Xmx256m"

# 7. COMANDO FINAL MATADOR: Inicia o Java já amarrado com a porta do Render e a memória.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:8080} -jar app.jar"]