$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) { throw 'Install JDK 21 or newer and add its bin folder to PATH.' }
New-Item -ItemType Directory -Force build | Out-Null
$taskSources = @(Get-ChildItem support,Task* -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& javac -Xlint:all -d build @taskSources 2>&1 | Tee-Object compilation.txt
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
@("UTC: $([DateTime]::UtcNow.ToString('o'))", (& java -version 2>&1), (& javac -version 2>&1), "OS: $([Environment]::OSVersion)", "CPUs: $([Environment]::ProcessorCount)") | Set-Content environment.txt
$taskClasses = @('task01.TicketBenchmark','task02.PlaylistBenchmark','task03.Attendance','task04.Inventory','task05.RankingDemo','task06.Triage','task07.Redesign')
for ($taskIndex=0; $taskIndex -lt $taskClasses.Count; $taskIndex++) {
    $taskFolder = 'Task{0:D2}' -f ($taskIndex+1)
    New-Item -ItemType Directory -Force "$taskFolder/results" | Out-Null
    & java -Xms128m -Xmx1024m -ea -cp build $taskClasses[$taskIndex] | Tee-Object "$taskFolder/results/output.txt"
    if ($LASTEXITCODE -ne 0) { throw "$taskFolder failed" }
}
