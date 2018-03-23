#!/bin/bash

if [ "$(docker ps -aq -f name=swarmpitec)" ];
then
   	echo "Event collector already exists."
    if [ "$(docker ps -aq -f status=exited -f name=swarmpitec)" ];
    then
        echo "Event collector down. Starting ..."
        docker start swarmpitec
    else
        echo "Event collector running."
    fi
else
    echo "Creating event collector"
    docker run -d \
      --name swarmpitec \
      --env EVENT_ENDPOINT=http://192.168.65.1:3449/events \
      --env HEALTH_CHECK_ENDPOINT=http://192.168.65.1:3449/version \
      --volume /var/run/docker.sock:/var/run/docker.sock \
      swarmpit/event-collector:latest
fi