# =========================================
# TEST NOTIFICATION WINDOWS - BOOSTUP
# =========================================
# Ce script teste si les notifications Windows fonctionnent
# indépendamment de l'application Java

Write-Host "`n🔔 Test de notification Windows..." -ForegroundColor Cyan

try {
    [Windows.UI.Notifications.ToastNotificationManager, Windows.UI.Notifications, ContentType = WindowsRuntime] | Out-Null
    [Windows.Data.Xml.Dom.XmlDocument, Windows.Data.Xml.Dom.XmlDocument, ContentType = WindowsRuntime] | Out-Null

    $APP_ID = 'BoostUp.EventManager'

    $template = @"
<toast>
    <visual>
        <binding template="ToastGeneric">
            <text>🎉 TEST - BoostUp Notifications</text>
            <text>Si vous voyez ce message, les notifications Windows fonctionnent parfaitement ! 🚀</text>
        </binding>
    </visual>
    <audio src="ms-winsoundevent:Notification.Default" />
</toast>
"@

    $xml = New-Object Windows.Data.Xml.Dom.XmlDocument
    $xml.LoadXml($template)
    $toast = New-Object Windows.UI.Notifications.ToastNotification $xml
    [Windows.UI.Notifications.ToastNotificationManager]::CreateToastNotifier($APP_ID).Show($toast)

    Write-Host "✅ Notification envoyée avec succès !" -ForegroundColor Green
    Write-Host "   Regardez en bas à droite de votre écran..." -ForegroundColor Yellow

} catch {
    Write-Host "❌ ERREUR lors de l'envoi de la notification :" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Write-Host "`n💡 Vérifiez que :" -ForegroundColor Yellow
    Write-Host "   1. Les notifications Windows sont activées (Paramètres > Système > Notifications)" -ForegroundColor White
    Write-Host "   2. Vous êtes sur Windows 10/11" -ForegroundColor White
    Write-Host "   3. PowerShell a les permissions nécessaires" -ForegroundColor White
}

Write-Host "`n✨ Test terminé.`n" -ForegroundColor Cyan

