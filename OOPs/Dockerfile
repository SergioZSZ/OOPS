FROM tomcat:10-jdk17

RUN apt-get update && apt-get install -y --no-install-recommends maven  

COPY . /oops

WORKDIR /oops/

RUN mvn package -Dmaven.test.skip=true

COPY tomcat-users.xml /usr/local/tomcat/conf/

RUN cp target/oops-0.3.0-SNAPSHOT.war /usr/local/tomcat/webapps/

EXPOSE 8080

CMD ["catalina.sh", "run"]


