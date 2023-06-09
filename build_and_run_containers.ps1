Set-location $PSScriptRoot

$separator = "===============================================================> "
Write-Output ($separator + "BUILD SW WITH GRADLE")
Start-Process -NoNewWindow -Wait -FilePath "./gradlew" -ArgumentList "clean installDist"
Write-Output ($separator + "BUILD CONTAINERS WITH DOCKER COMPOSE")
Start-Process -NoNewWindow -Wait -FilePath "docker.exe" -ArgumentList "compose -f ./env/docker-compose.yml build"
Write-Output ($separator + "START CONTAINERS WITH DOCKER COMPOSE")
Start-Process -NoNewWindow -Wait -FilePath "docker.exe" -ArgumentList "compose -f ./env/docker-compose.yml up -d"
Write-Output ($separator + "WAITING 10 SECONDS")
Start-Sleep -Seconds 10
Write-Output ($separator + "OPENING WEB BROWSER ON http://127.0.0.1:9000/prototype/main/")
Start-Process "http://127.0.0.1:9000/prototype/main/"

Write-Output "" ($separator + "DONE.") ""
