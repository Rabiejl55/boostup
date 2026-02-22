# NOTIFICATION WINDOWS DIRECTE - SANS BLOCAGE
# Ce script force l'affichage des notifications même si l'app n'est pas installée

param(
    [string]$Title = "Rappel Evenement - BoostUp",
    [string]$Message = "Un événement approche !",
    [string]$Details = "Vérifiez vos événements"
)

Write-Host "Envoi de notification Windows..." -ForegroundColor Cyan
Write-Host "   Titre   : $Title" -ForegroundColor White
Write-Host "   Message : $Message" -ForegroundColor White
Write-Host "   Details : $Details" -ForegroundColor White

try {
    # Méthode 1 : Notification Tray (basique mais fonctionne toujours)
    Add-Type -AssemblyName System.Windows.Forms
    $notification = New-Object System.Windows.Forms.NotifyIcon
    $notification.Icon = [System.Drawing.SystemIcons]::Information
    $notification.BalloonTipTitle = $Title
    $notification.BalloonTipText = "$Message`n$Details"
    $notification.Visible = $True
    $notification.ShowBalloonTip(10000)

    Write-Host "Notification envoyee avec succes!" -ForegroundColor Green
    Start-Sleep -Seconds 1
    $notification.Dispose()
}
catch {
    Write-Host "Erreur lors de l'envoi de la notification : $_" -ForegroundColor Red
    exit 1
}

Write-Host "Termine ! Regarde en bas a droite !" -ForegroundColor Cyan
exit 0


