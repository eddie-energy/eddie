Set-location $PSScriptRoot

$separator = "===============================================================> "
Write-Output ($separator + "BUILD SW WITH GRADLE")
Start-Process -NoNewWindow -Wait -FilePath "./gradlew" -ArgumentList "clean installDist"
Write-Output ($separator + "BUILD CONTAINERS WITH DOCKER COMPOSE")
Start-Process -NoNewWindow -Wait -FilePath "docker" -ArgumentList "compose -f ./env/docker-compose.yml build"
Write-Output ($separator + "START CONTAINERS WITH DOCKER COMPOSE")
Start-Process -NoNewWindow -Wait -FilePath "docker" -ArgumentList "compose -f ./env/docker-compose.yml up -d"
Write-Output ($separator + "WAITING 10 SECONDS")
Start-Sleep -Seconds 10
$url = "http://127.0.0.1:9000/prototype/main/"
Write-Output ($separator + "OPENING WEB BROWSER ON " + $url)
if ($env:OS -eq "Windows_NT")
{
    Start-Process $url
}
elseif (Get-Command "xdg-open" -ErrorAction SilentlyContinue)
{
    Start-Process "xdg-open " -ArgumentList $url
}
elseif (Get-Command "start" -ErrorAction SilentlyContinue)
{
    Start-Process "start " -ArgumentList $url
}

Write-Output "" ($separator + "DONE.") ""
