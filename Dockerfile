FROM gradle:7.6-jdk8 AS build
WORKDIR /app
COPY . .
RUN gradle war --no-daemon

FROM tomcat:9-jdk8
ENV DB_HOST=sqlserver
ENV DB_PORT=1433
ENV DB_NAME=ZavaBankDB
ENV DB_USER=sa
ENV DB_PASSWORD=YourStrong!Passw0rd
COPY --from=build /app/build/libs/*.war /usr/local/tomcat/webapps/ROOT.war
RUN rm -rf /usr/local/tomcat/webapps/ROOT
EXPOSE 8080
CMD ["catalina.sh", "run"]
