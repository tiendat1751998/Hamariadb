# Stage 1: Sử dụng base image có JDK 17 để chạy ứng dụng
FROM eclipse-temurin:17-jdk-jammy

# Đặt các biến cho user/group để chạy dưới quyền non-root (bảo mật hơn)
ARG APP_USER=hamaria
ARG APP_GROUP=hamaria
ARG UID=1001
ARG GID=1001

# Tạo user và group non-root
RUN groupadd -g ${GID} ${APP_GROUP} && \
    useradd -u ${UID} -g ${APP_GROUP} -s /bin/sh ${APP_USER}

# Đặt thư mục làm việc
WORKDIR /app

# Sao chép file JAR đã được build từ thư mục target vào container
# Tên file JAR có thể thay đổi, nên dùng ký tự đại diện *.jar
COPY /target/*.jar app.jar

# Thay đổi quyền sở hữu của thư mục và file JAR
RUN chown -R ${APP_USER}:${APP_GROUP} /app

# Chuyển sang user non-root
USER ${APP_USER}

# Mở port mà ứng dụng sẽ chạy
EXPOSE 8080

# Lệnh để khởi chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]