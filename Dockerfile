FROM ubuntu:18.04
MAINTAINER Roman Shishkin <romashkin.2001@yandex.ru>
LABEL Description="This image is for grading system for TRIK Studio testing system"

#Updating system and installing packages
WORKDIR /

RUN echo 'APT::Install-Recommends "0";' > /etc/apt/apt.conf.d/99norecommends \
    && apt-get -y update \
    && apt-get -y install \
    apt-utils \
    default-jdk \
    && apt-get autoremove -y \
    && apt-get clean -y


#Copying application
WORKDIR /$APP_DIR
ARG JAR_FILE=build/libs/trik-testsys-grading-system-3.0.0.jar
ARG APP=app.jar
COPY $JAR_FILE $APP

#Running application
EXPOSE 8080
ENTRYPOINT java -jar app.jar
