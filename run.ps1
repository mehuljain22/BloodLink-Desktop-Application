$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force out | Out-Null
$files = Get-ChildItem -Recurse src/main/java -Filter *.java | ForEach-Object { $_.FullName }
javac -d out $files
java -cp out com.bloodlink.Main
