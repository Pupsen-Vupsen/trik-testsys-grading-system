FROM --platform=linux/amd64 ubuntu:18.04
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

#Updating system and installing packages
WORKDIR /
ARG DEBIAN_FRONTEND=noninteractive
RUN echo 'APT::Install-Recommends "0";' > /etc/apt/apt.conf.d/99norecommends \
    && apt-get -y update \
    && apt-get -y install \
    apt-utils \
    default-jdk \
    wget \
    curl \
    libpulse0 \
    locales \
    libxkbcommon-x11-0 \
    && apt-get autoremove -y \
    && apt-get clean -y

RUN echo 'ru_RU.UTF-8 UTF-8' > /etc/locale.gen \
   && locale-gen
ENV LANG ru_RU.UTF-8
ENV LANGUAGE ru_RU:ru
ENV LC_LANG ru_RU.UTF-8
ENV LC_ALL ru_RU.UTF-8

#Copying intalling script for master installer
WORKDIR /$APP_DIR/$INSTALLER_DIR
COPY docker/trik_studio_installscript.qs install_script.qs

#Downloading TRIK Studio
WORKDIR /$APP_DIR/$INSTALLER_DIR
RUN curl --output $INSTALLER $MASTER_INSTALLER_URL

#Setting installer executable
WORKDIR /$APP_DIR/$INSTALLER_DIR
RUN chmod +x $INSTALLER

#Installing TRIKStudio
WORKDIR /$APP_DIR
#Command to install master version
RUN env INSTALL_DIR=/$APP_DIR/$TRIK_STUDIO_DIR ./$INSTALLER_DIR/$INSTALLER --script ./$INSTALLER_DIR/install_script.qs --platform minimal --verbose
#Command to install release version
#RUN ./$INSTALLER_DIR/$INSTALLER --am --al --confirm-command -t /$APP_DIR/$TRIK_STUDIO_DIR install

#Checking TRIK Studio version
WORKDIR /$APP_DIR/$TRIK_STUDIO_DIR
RUN ./trik-studio -platform offscreen --version | grep -F  \
    $(curl -s https://api.github.com/repos/trikset/trik-studio/commits/master | grep sha | head -n 1 | cut -d '"' -f 4 | cut -c 1-6)

#Removing installer and script
WORKDIR /$APP_DIR
RUN rm -r $INSTALLER_DIR

##Copying tasks (only for testing container)
#WORKDIR /
#COPY tasks tasks

##Creating directory for submissions
#WORKDIR /
#RUN mkdir submissions

#Copying application
WORKDIR /$APP_DIR
ARG JAR_FILE=build/libs/trik-testsys-2.0.11.jar
ARG APP=app.jar
COPY $JAR_FILE $APP

#Running application
EXPOSE 8080
ENTRYPOINT java -jar app.jar
