#!/usr/bin/env bash
cd $(dirname $0)

separator="===============================================================> "
echo $separator "BUILD SW WITH GRADLE"
./gradlew clean installDist
echo $separator "BUILD CONTAINERS WITH DOCKER COMPOSE"
docker compose -f ./env/docker-compose.yml build
echo $separator "START CONTAINERS WITH DOCKER COMPOSE"
docker.exe compose -f ./env/docker-compose.yml up -d
echo $separator "WAITING 10 SECONDS"
sleep 10
echo $separator "OPENING WEB BROWSER ON http://127.0.0.1:9000/prototype/main/"

for open_command in "xdg-open" "open" "start"; do
  if command -v $open_command &> /dev/null; then
    $open_command "http://127.0.0.1:9000/prototype/main/"
  fi
done

echo $separator "DONE."
