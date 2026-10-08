$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { '' }
$compiler = if ($javaBin -and (Test-Path (Join-Path $javaBin 'javac.exe'))) {
    Join-Path $javaBin 'javac.exe'
} else {
    (Get-Command javac -ErrorAction Stop).Source
}
$launcher = if ($javaBin -and (Test-Path (Join-Path $javaBin 'java.exe'))) {
    Join-Path $javaBin 'java.exe'
} else {
    (Get-Command java -ErrorAction Stop).Source
}

$javaFiles = Get-ChildItem -Path 'src\main\java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force -Path 'target\classes' | Out-Null
& $compiler -encoding UTF-8 -d 'target\classes' $javaFiles
if ($LASTEXITCODE -ne 0) {
    throw 'Java compilation failed.'
}

& $launcher -cp 'target\classes' edu.homeautomation.Main
