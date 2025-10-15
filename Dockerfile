# --- GIAI ĐOẠN 1: BUILD ---
# Đặt tên cho giai đoạn này là "builder"
FROM maven:3.9-eclipse-temurin-17 AS builder

# Đặt thư mục làm việc
WORKDIR /app

# SỬA LỖI: Copy file pom.xml từ thư mục con Hamariadb
COPY Hamariadb/pom.xml ./pom.xml
RUN mvn dependency:go-offline

# SỬA LỖI: Copy mã nguồn từ thư mục con Hamariadb
COPY Hamariadb/src ./src
RUN mvn clean package -DskipTests


# --- GIAI ĐOẠN 2: RUNTIME ---
# Sử dụng một base image nhẹ hơn chỉ chứa JRE để chạy
FROM eclipse-temurin:17-jre-jammy

# Các biến cho user non-root
ARG APP_USER=hamaria
ARG APP_GROUP=hamaria
ARG UID=1001
ARG GID=1001

# Tạo user và group non-root
RUN groupadd -g ${GID} ${APP_GROUP} && \
    useradd -u ${UID} -g ${APP_GROUP} -s /bin/sh ${APP_USER}

# Đặt thư mục làm việc
WORKDIR /app

# SỬA LỖI: Copy file JAR từ đúng đường dẫn trong stage "builder"
COPY --from=builder /app/target/*.jar app.jar

# Thay đổi quyền sở hữu
RUN chown -R ${APP_USER}:${APP_GROUP} /app

# Chuyển sang user non-root
USER ${APP_USER}

# Mở port
EXPOSE 8080

# Lệnh chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]