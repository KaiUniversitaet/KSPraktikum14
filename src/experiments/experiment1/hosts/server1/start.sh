#!/bin/bash

# Hier Pfad zu den Java-class-files eintragen
export PATH_TO_CLASSFILES="/home/praktikum/IdeaProjects/KSPraktikum14/out/production/KSPraktikum14"

groovy -cp $PATH_TO_CLASSFILES":../../../../../libs/jpcap.jar" -D stand.alone Server.groovy
