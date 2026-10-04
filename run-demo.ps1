$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$javaFiles = Get-ChildItem -Path 'src\main\java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force -Path 'target\classes' | Out-Null
& javac -encoding UTF-8 -d 'target\classes' $javaFiles
if ($LASTEXITCODE -ne 0) {
    throw 'Java compilation failed.'
}

& java -cp 'target\classes' edu.homeautomation.Main
