FROM eclipse-temurin:17
COPY TransferServer.jar mappings.json json-20240205.jar ./
EXPOSE 8086
CMD ["java", "-cp", "json-20240205.jar:TransferServer.jar", "TransferServer"]

