FROM ubuntu:18.04
MAINTAINER Roman Shishkin <romashkin.2001@yandex.ru>
LABEL Description="This image is for grading system for TRIK Studio testing system"

ARG APP_DIR=grading-system
ARG TRIK_STUDIO_DIR=TRIKStudio

ARG INSTALLER=trik-studio-installer-gnu64.run

#Updating system
RUN apt -y update

#Installing openjdk
RUN apt install -y default-jdk

#Installing wget
RUN apt install -y wget

#Installing QT5
RUN apt install -y qt5-default

#Installing TRIK Studio
WORKDIR $TRIK_STUDIO_DIR/installer
RUN wget https://dl.trikset.com/ts/$INSTALLER
RUN chmod +x ./$INSTALLER
RUN ./$INSTALLER --am --al --confirm-command -t /$APP_DIR/$TRIK_STUDIO_DIR install
RUN rm $INSTALLER

#Copying scripts
WORKDIR ../../../$APP_DIR
COPY echo_pin.sh echo_pin.sh
COPY generate_hash.sh generate_hash.sh

#Copying tasks (only for testing container)
#COPY tasks tasks

#Copying and running application
ARG JAR_FILE=build/libs/trik-testsys-0.1.9.jar
ARG APP=app.jar
COPY $JAR_FILE $APP
EXPOSE 8080
ENTRYPOINT java -jar app.jar