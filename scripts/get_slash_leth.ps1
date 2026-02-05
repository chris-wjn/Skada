$jsonPath = 'e:\MODDING\Minecraft Modding\Skada\run\config\skada\weapons\generated\minecraft.json'
$json = Get-Content -Raw -Path $jsonPath | ConvertFrom-Json

$items = $json.PSObject.Properties |
  ForEach-Object {
    $name = $_.Name
    $val  = $_.Value
    if ($val.attack_types -and $val.attack_types.'skada:slash') {
      $lethVal = $val.attack_types.'skada:slash'.lethality
      [PSCustomObject]@{
        Name = $name
        Lethality = if ($lethVal -is [string]) { [double]::Parse($lethVal) } else { [double]$lethVal }
      }
    }
  } |
  Sort-Object -Property Lethality -Descending

if (-not $items -or $items.Count -eq 0) {
  Write-Output "No slash lethality values found."
  exit 0
}

# $items - print each
$items | ForEach-Object {
  Write-Output ("{0}: {1}" -f $_.Name, $_.Lethality)
}

# compute range (max - min)
$max = $items[0].Lethality
$min = $items[$items.Count - 1].Lethality
$range = $max - $min

# compute average difference between consecutive values (absolute diffs)
if ($items.Count -gt 1) {
  $diffs = for ($i = 0; $i -lt $items.Count - 1; $i++) {
    [math]::Abs($items[$i].Lethality - $items[$i + 1].Lethality)
  }
  $avg = ($diffs | Measure-Object -Average).Average
  $avgRounded = [math]::Round($avg, 6)
} else {
  $avgRounded = "N/A"
}

Write-Output ""
Write-Output ("Difference between highest and lowest lethality: {0}" -f $range)
Write-Output ("Average difference between consecutive lethality values: {0}" -f $avgRounded)