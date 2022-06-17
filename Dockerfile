FROM ubuntu:18.04
MAINTAINER Roman Shishkin <romashkin.2001@yandex.ru>
LABEL Description="This image is for grading system for TRIK Studio testing system"

#Setting directories args
ARG APP_DIR=grading-system
ARG TRIK_STUDIO_DIR=TRIKStudio
ARG INSTALLER_DIR=installer

#Setting installers args
ARG INSTALLER=installer.run
ARG RELEASE_INSTALLER_URL=https://dl.trikset.com/ts/trik-studio-installer-gnu64.run
ARG MASTER_INSTALLER_URL=https://dl.trikset.com/ts/fresh/installer/trik-studio-installer-linux-master.run

#Updating system
WORKDIR /
RUN apt-get -y update

#Installing utils
WORKDIR /
RUN apt-get -y install apt-utils default-jdk wget curl qt5-default

#Copying intalling script for master installer
WORKDIR /$APP_DIR/$INSTALLER_DIR
COPY docker/trik_studio_installscript.qs install_script.qs

#Downloading TRIK Studio
WORKDIR /$APP_DIR/$INSTALLER_DIR
RUN curl --output $INSTALLER $MASTER_INSTALLER_URL

#Setting installer executable
WORKDIR /$APP_DIR/$INSTALLER_DIR
RUN chmod +x $INSTALLER

#Checking installer sha1 sum
WORKDIR /$APP_DIR/$INSTALLER_DIR
ARG INSTALLER_SHA1_SUM=c0732c4
RUN ./$INSTALLER --version | grep -F $INSTALLER_SHA1_SUM

#Installing TRIKStudio
WORKDIR /$APP_DIR
#Command to install master version
RUN env INSTALL_DIR=/$APP_DIR/$TRIK_STUDIO_DIR ./$INSTALLER_DIR/$INSTALLER --script ./$INSTALLER_DIR/install_script.qs --platform minimal --verbose
#Command to install release version
#RUN ./$INSTALLER --am --al --confirm-command -t /$APP_DIR/$TRIK_STUDIO_DIR install

#Checking TRIK Studio version
WORKDIR /$APP_DIR/$TRIK_STUDIO_DIR
ARG TRIK_STUDIO_VERSION=2022.1-7-g3e1af1
RUN ./trik-studio -platform offscreen --version | grep -F $TRIK_STUDIO_VERSION

#Removing installer and script
WORKDIR /$APP_DIR
RUN rm -r $INSTALLER_DIR

#Copying scripts
WORKDIR /$APP_DIR
COPY docker/echo_pin.sh echo_pin.sh
COPY docker/generate_hash.sh generate_hash.sh

##Copying tasks (only for testing container)
#WORKDIR /$APP_DIR
#COPY tasks tasks

# Creating directory for submissions
WORKDIR /$APP_DIR
RUN mkdir submissions

#Copying application
WORKDIR /$APP_DIR
ARG JAR_FILE=build/libs/trik-testsys-1.0.1.jar
ARG APP=app.jar
COPY $JAR_FILE $APP

#Running application
EXPOSE 8080
ENTRYPOINT java -jar app.jar