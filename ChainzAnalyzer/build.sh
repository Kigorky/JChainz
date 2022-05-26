mvn clean install
mvn assembly:assembly -DdescriptorId=jar-with-dependencies


echo "# Data dependency analysis
REACHING_DEFINITIONS_SAME_VAR_EDGE=[color=\"#0000ff\"]
DEF_DEPENDS_ON_USE_EDGE=[color=\"#ff0000\"]
INTER_METHOD_PARAMETER_EDGE=[style=dashed]
INTER_METHOD_OBJECT_REFERENCE_EDGE=[style=dashed,color=\"#00ff00\"]
REACHING_DEFINITIONS_OTHER_VAR_EDGE=[style=dashed,color=\"#0000ff\"]
predecessorColor=#00ff00
successorColor=#ff0000

# Common properties
JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
PROJECT_HOME=/home/andromeda/jchainz

targetJarPath=" > config.properties
