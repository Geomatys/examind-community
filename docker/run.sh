#!/bin/bash

docker-compose stop
docker-compose rm
docker-compose up -d


docker-compose logs -f --tail=800 examind