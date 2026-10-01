$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-1.8'
Set-Location 'D:\downloads\algs4-master\algs4-master'
& mvn -o `
    "-Dtest=DirectAddressBlindTest,DivisionHashBlindTest,DifferentialBlindTest" `
    "-DfailIfNoTests=false" `
    "-Dmaven.repo.local=D:\maven-repo" `
    test
Write-Output ("EXITCODE=" + $LASTEXITCODE)
