#!/bin/bash
OR_ROOT_DIR=/opt/ORPixip
LIB_DIR=$OR_ROOT_DIR/lib
OR_LIB_DIR=$OR_ROOT_DIR/ORlib
APP_LIB_DIR=$OR_ROOT_DIR/dist
PROPERTIES_DIR=$OR_ROOT_DIR/properties
JDKPath=/opt/jdk1.7.0

#Build classpath
for libfile in $LIB_DIR/*.jar; do
    CLASSPATH=$CLASSPATH:$libfile
done

# Add the openrate jar and sources
for libfile in $OR_LIB_DIR/*.jar; do
    CLASSPATH=$CLASSPATH:$libfile
done

# Add only the application jar we want
CLASSPATH=$CLASSPATH:$APP_LIB_DIR/ORPixip-1.0.0.jar

# Add the properties
CLASSPATH=$CLASSPATH:$PROPERTIES_DIR

echo $CLASSPATH

cd $OR_ROOT_DIR
$JDKPath/bin/java -Xms256m -Xmx512m -cp $CLASSPATH -server OpenRate.OpenRate -p Pixip.properties.xml &

