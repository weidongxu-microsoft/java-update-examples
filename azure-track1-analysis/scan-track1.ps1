$ErrorActionPreference='SilentlyContinue'
Set-Location "C:\github\java-update-examples\azure-track1-analysis"
$targets = @(
 @{Name='hadoop'; Repo='hadoop'; Scope='hadoop-tools/hadoop-azure'},
 @{Name='hbase'; Repo='hbase'; Scope='.'},
 @{Name='delta'; Repo='delta'; Scope='.'},
 @{Name='eventhubs-spark'; Repo='eventhubs-spark'; Scope='.'},
 @{Name='camel'; Repo='camel'; Scope='components/camel-azure'},
 @{Name='azure-sdk-for-java'; Repo='azure-sdk-for-java'; Scope='sdk'}
)
$pkgPattern='com\\.microsoft\\.azure\\.(storage|keyvault|eventhubs|servicebus)'
$artifactPattern='com\\.microsoft\\.azure:azure-(storage|storage-blob|keyvault|eventhubs|servicebus)|<groupId>com\\.microsoft\\.azure</groupId>|<artifactId>azure-(storage|storage-blob|keyvault|eventhubs|servicebus)</artifactId>'
$rows=@()
foreach($t in $targets){
  $repoPath = Join-Path (Get-Location) $t.Repo
  if(-not (Test-Path $repoPath)){ continue }
  $scopePath = if($t.Scope -eq '.') { $repoPath } else { Join-Path $repoPath $t.Scope }
  if(-not (Test-Path $scopePath)){ continue }
  $codeFiles = Get-ChildItem -Path $scopePath -Recurse -File -Include *.java,*.scala,*.kt,*.groovy
  $codeMatches = $codeFiles | Select-String -Pattern $pkgPattern
  $importMatches = $codeFiles | Select-String -Pattern ('^\s*import\s+' + $pkgPattern)
  $buildFiles = Get-ChildItem -Path $scopePath -Recurse -File -Include pom.xml,build.gradle,build.gradle.kts,*.sbt,libs.versions.toml,gradle.properties
  $buildMatches = $buildFiles | Select-String -Pattern $artifactPattern
  $usageFiles = ($codeMatches | Select-Object -ExpandProperty Path -Unique | Measure-Object).Count
  $usageLines = ($codeMatches | Measure-Object).Count
  $importLines = ($importMatches | Measure-Object).Count
  $depFiles = ($buildMatches | Select-Object -ExpandProperty Path -Unique | Measure-Object).Count
  $depLines = ($buildMatches | Measure-Object).Count
  $imports = $importMatches | ForEach-Object { $_.Line.Trim() } | Sort-Object -Unique
  $sdkKinds = @()
  if($imports -match 'com\.microsoft\.azure\.storage'){ $sdkKinds += 'storage' }
  if($imports -match 'com\.microsoft\.azure\.keyvault'){ $sdkKinds += 'keyvault' }
  if($imports -match 'com\.microsoft\.azure\.eventhubs'){ $sdkKinds += 'eventhubs' }
  if($imports -match 'com\.microsoft\.azure\.servicebus'){ $sdkKinds += 'servicebus' }
  $sdkKinds = $sdkKinds | Sort-Object -Unique
  $rows += [PSCustomObject]@{Repo=$t.Name; Scope=$t.Scope; UsageFiles=$usageFiles; UsageLines=$usageLines; ImportLines=$importLines; DepFiles=$depFiles; DepLines=$depLines; SDKs=($sdkKinds -join ',')}
}
$rows = $rows | Sort-Object UsageLines -Descending
$rows | Format-Table -AutoSize
"JSON_START"
$rows | ConvertTo-Json -Depth 4
