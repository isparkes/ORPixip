OR_ROOT_DIR=/opt/ORPixip
JDKPath=/opt/jdk1.7.0
LIB_DIR=$OR_ROOT_DIR/lib
OR_LIB_DIR=$OR_ROOT_DIR/ORlib
/opt/jdk1.6.0/bin/java -cp $OR_LIB_DIR/ORCLClient.jar simpleclient.OpenRateClient localhost 8086 Framework:Shutdown=true

