$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$javaBin = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { '' }
$compiler = if ($javaBin -and (Test-Path (Join-Path $javaBin 'javac.exe'))) {
    Join-Path $javaBin 'javac.exe'
} else { (Get-Command javac -ErrorAction Stop).Source }
$launcher = if ($javaBin -and (Test-Path (Join-Path $javaBin 'java.exe'))) {
    Join-Path $javaBin 'java.exe'
} else { (Get-Command java -ErrorAction Stop).Source }
$servletApi = Join-Path $env:USERPROFILE '.m2\repository\jakarta\servlet\jakarta.servlet-api\6.0.0\jakarta.servlet-api-6.0.0.jar'
if (-not (Test-Path $servletApi)) {
    throw 'Jakarta Servlet API 6.0.0 was not found in the Maven cache. Build the web project once with Maven, then run these checks again.'
}

$output = Join-Path $PSScriptRoot 'target\checks'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$sources = Get-ChildItem 'src\main\java','web\src\main\java','src\test\java','web\src\test\java' -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
& $compiler -encoding UTF-8 -cp $servletApi -d $output $sources
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Review the Java errors above.' }
$classpath = "$output;$servletApi"
& $launcher -ea -cp $classpath edu.homeautomation.service.CoreFeatureChecks
if ($LASTEXITCODE -ne 0) { throw 'Core feature checks failed.' }
& $launcher -ea -cp $classpath edu.homeautomation.web.WebFeatureChecks
if ($LASTEXITCODE -ne 0) { throw 'Web feature checks failed.' }
