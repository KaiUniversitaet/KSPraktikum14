#!/bin/bash

# Hier Pfad zu den Java-class-files eintragen
export PATH_TO_CLASSFILES="/home/kai/IdeaProjects/KSPraktikum14/out/production/KSPraktikum14"

groovy -cp $PATH_TO_CLASSFILES":../../../../../libs/jpcap.jar" -D stand.alone Client.groovy

