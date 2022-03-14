FROM ubuntu:18.04
MAINTAINER Roman Shishkin <romashkin.2001@yandex.ru>
LABEL Description="This image is for grading system for TRIK Studio testing system"

ARG APP_DIR=grading-system
ARG TRIK_STUDIO_DIR=TRIKStudio

ARG INSTALLER=trik-studio-installer-gnu64.run

ARG JAR_FILE=build/libs/trik-testsys-0.1.2.jar
ARG APP=app.jar

#Updating system
RUN apt -y update

#Installing openjdk
RUN apt install -y default-jdk

#Installing wget
RUN apt install -y wget

#Installing QT5
RUN apt install -y qt5-default

#Copying application
WORKDIR $APP_DIR
COPY $JAR_FILE $APP

#Copying tasks
COPY ./tasks tasks

#Installing TRIK Studio
WORKDIR $TRIK_STUDIO_DIR/installer
RUN wget https://dl.trikset.com/ts/$INSTALLER
RUN chmod +x ./$INSTALLER
RUN ./$INSTALLER --am --al --confirm-command -t /$APP_DIR/$TRIK_STUDIO_DIR install
RUN rm $INSTALLER

#Running application
WORKDIR ../../../$APP_DIR
EXPOSE 8080
ENTRYPOINT java -jar app.jar