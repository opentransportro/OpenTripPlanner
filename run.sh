#!/bin/bash

# Fail fast if graph building fails
set -e

JAR=`ls *-shaded*`
echo JAR=$JAR
SLEEP_TIME=5

#Parse data container URL(s) to a bash array
IFS=',' read -r -a ROUTER_DATA_CONTAINER_URLS <<< $ROUTER_DATA_CONTAINER_URL

#Parse router name(s) to a bash array
IFS=',' read -r -a ROUTER_NAMES <<< $ROUTER_NAME

ROUTERS=$(printf " --router %s" ${ROUTER_NAMES[@]})

function url {
  echo "${ROUTER_DATA_CONTAINER_URLS[$1]}"/$2
}

function build_graph {
  GRAPHNAME=$1
  FILE=$2
  echo "building graph..."
  DIR="graphs"
  rm -rf $DIR/$GRAPHNAME || true
  mkdir -p $DIR
  unzip -o -d $DIR $FILE
  mv $DIR/graph-$GRAPHNAME $DIR/$GRAPHNAME
  java $JAVA_OPTS -jar $JAR --build $DIR/$GRAPHNAME
}

function download_graph {
  NAME=$1
  VERSION=$2
  GRAPH_FILE=graph-$NAME.zip
  URL=$(url $3 "graph-$NAME.zip")
  echo "Downloading graph from $URL"
  for i in {1..6}; do
    HTTP_STATUS=$(curl --write-out %{http_code} --silent --output $GRAPH_FILE $URL)

    if [ "$HTTP_STATUS" -eq 404 ]; then
        rm  $GRAPH_FILE > /dev/null
        echo "Graph not found";
       return 1;
    fi

    if [ "$HTTP_STATUS" -eq 200 ]; then break;
    fi;

    rm  $GRAPH_FILE > /dev/null
    sleep $SLEEP_TIME;

  done

  if [ -f graph-$NAME.zip ]; then
    # if the graph already exists then overwrite it, otherwise we need to build a new one on every start
    unzip -o $GRAPH_FILE  -d ./graphs
    return $?;
  else
    return 1;
  fi
}

function version {
  java -jar $JAR --version|grep commit|cut -d' ' -f2
}

function process {
  NAME=$1
  URL=$(url $2 "graph-$NAME.zip")
  FILE="$NAME.zip"

  echo "Retrieving graph source bundle from $URL"
  until curl -f -s $URL -o $FILE
  do
    echo "Error retrieving graph source bundle $URL from otp-data-server... retrying in $SLEEP_TIME s..."
    sleep $SLEEP_TIME
  done

  build_graph $NAME $FILE
}

#workaround for azure DNS issue

if [ -n "$MESOS_CONTAINER_NAME"  ]; then 
  echo "search marathon.l4lb.thisdcos.directory" >> /etc/resolv.conf
fi

VERSION=$(version)

echo VERSION $VERSION

j="0"

for ROUTER in "${ROUTER_NAMES[@]}"
do
  echo ROUTER $ROUTER

  download_graph $ROUTER $VERSION "$j" || process $ROUTER "$j"

  echo "graphString is: $ROUTER"
  j=$[$j+1]
done

java -Dsentry.release=$VERSION $JAVA_OPTS -Duser.timezone=Europe/Bucharest -jar $JAR --server --port $PORT --securePort $SECURE_PORT --basePath ./ --graphs ./graphs $ROUTERS
