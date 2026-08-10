<#
.SYNOPSIS
    Создание MailContact в Active Directory через удалённую сессию Exchange (On-Premise),
    чтобы объект корректно появился в GAL (Global Address List).

.DESCRIPTION
    Скрипт подключается к Exchange по WinRM (Remote PowerShell, HTTPS) и создаёт
    контакт командлетом New-MailContact — единственный надёжный способ получить
    объект, который Exchange полноценно распознаёт (msExchRecipientTypeDetails,
    proxyAddresses, попадание в OAB/GAL).

    ПК, с которого запускается скрипт, НЕ обязан быть в домене — достаточно
    сетевого доступа (443/HTTPS) до сервера Exchange (CAS, виртуальный каталог
    /PowerShell). Аутентификация Basic (по HTTPS) вместо Kerberos.

.EXAMPLE
    powershell -NoProfile -ExecutionPolicy Bypass -File .\New-MailContactViaExchange.ps1 `
        -ExchangeServerFQDN "exch01.contoso.local" `
        -ExchangeUser "CONTOSO\svc_provisioning" `
        -ExchangePassword "P@ssw0rd" `
        -ContactName "Иванов Иван Иванович" `
        -ContactAlias "ivanov_ii" `
        -ContactFirstName "Иван" `
        -ContactLastName "Иванов" `
        -ContactExternalEmail "ivanov.ii@external-domain.com" `
        -ContactOU "OU=Contacts,OU=Corp,DC=contoso,DC=local" `
        -ContactCompany "External Company LLC" `
        -ContactDepartment "Partners" `
        -ContactTitle "Manager" `
        -ContactPhone "+7 000 000-00-00" `
        -HideFromGAL:$false `
        -SkipCertificateCheck

.NOTES
    Вызов из кода:
        powershell -NoProfile -ExecutionPolicy Bypass -File "New-MailContactViaExchange.ps1"
                    -ExchangeServerFQDN "exch01.contoso.local"
                    -ExchangeUser "CONTOSO\svc"
                    -ExchangePassword "..."
                    -ContactName "..." -ContactAlias "..." ...

    Скрипт возвращает код завершения (exit code):
        0 - успех
        1 - ошибка подключения/создания (см. stderr и JSON в stdout / -OutputJsonPath)
#>

[CmdletBinding()]
param(
    # --- Подключение к Exchange ---
    [Parameter(Mandatory = $true)]
    [string]$ExchangeServerFQDN,

    [Parameter(Mandatory = $false)]
    [string]$ExchangeConnectionUri,     # если не задан - строится из ExchangeServerFQDN

    [Parameter(Mandatory = $true)]
    [string]$ExchangeUser,              # DOMAIN\user

    [Parameter(Mandatory = $true)]
    [string]$ExchangePassword,

    [Parameter(Mandatory = $false)]
    [ValidateSet("Basic", "Kerberos", "Negotiate", "NTLM", "CredSSP")]
    [string]$ExchangeAuthType = "Basic",

    [Parameter(Mandatory = $false)]
    [switch]$SkipCertificateCheck,      # пропустить проверку сертификата (самоподписанный/внутренний CA)

    # --- Параметры создаваемого MailContact ---
    [Parameter(Mandatory = $true)]
    [string]$ContactName,

    [Parameter(Mandatory = $true)]
    [string]$ContactAlias,

    [Parameter(Mandatory = $false)]
    [string]$ContactFirstName,

    [Parameter(Mandatory = $false)]
    [string]$ContactLastName,

    [Parameter(Mandatory = $true)]
    [string]$ContactExternalEmail,

    [Parameter(Mandatory = $true)]
    [string]$ContactOU,                 # точный DN контейнера/OU

    [Parameter(Mandatory = $false)]
    [string]$ContactCompany,

    [Parameter(Mandatory = $false)]
    [string]$ContactDepartment,

    [Parameter(Mandatory = $false)]
    [string]$ContactTitle,

    [Parameter(Mandatory = $false)]
    [string]$ContactPhone,

    [Parameter(Mandatory = $false)]
    [bool]$HideFromGAL = $false,

    # --- Служебное ---
    [Parameter(Mandatory = $false)]
    [string]$OutputJsonPath             # если указан - результат работы дополнительно пишется туда в формате JSON
)

$ErrorActionPreference = "Stop"
$result = [ordered]@{
    Success         = $false
    Message         = ""
    ContactIdentity = $null
    Error           = $null
}

function Write-Result {
    param($ResultObject)
    $json = $ResultObject | ConvertTo-Json -Depth 5
    if ($OutputJsonPath) {
        $json | Out-File -FilePath $OutputJsonPath -Encoding utf8 -Force
    }
    Write-Output $json
}

$ExchangeSession = $null

try {
    if (-not $ExchangeConnectionUri) {
        $ExchangeConnectionUri = "https://$ExchangeServerFQDN/PowerShell/"
    }

    Write-Verbose "Подключение к Exchange: $ExchangeConnectionUri"

    $securePassword = ConvertTo-SecureString $ExchangePassword -AsPlainText -Force
    $credential = New-Object System.Management.Automation.PSCredential ($ExchangeUser, $securePassword)

    $sessionOptionParams = @{}
    if ($SkipCertificateCheck) {
        $sessionOptionParams["SkipCACheck"] = $true
        $sessionOptionParams["SkipCNCheck"] = $true
        $sessionOptionParams["SkipRevocationCheck"] = $true
    }
    $sessionOption = New-PSSessionOption @sessionOptionParams

    $sessionParams = @{
        ConfigurationName = "Microsoft.Exchange"
        ConnectionUri     = $ExchangeConnectionUri
        Credential        = $credential
        Authentication    = $ExchangeAuthType
        SessionOption     = $sessionOption
        AllowRedirection  = $true
    }

    $ExchangeSession = New-PSSession @sessionParams

    Import-PSSession $ExchangeSession -DisableNameChecking -AllowClobber | Out-Null

    Write-Verbose "Подключение установлено. Создание MailContact '$ContactName'..."

    $newContactParams = @{
        Name                 = $ContactName
        Alias                = $ContactAlias
        DisplayName          = $ContactName
        ExternalEmailAddress = $ContactExternalEmail
        OrganizationalUnit   = $ContactOU
    }
    if ($ContactFirstName) { $newContactParams["FirstName"] = $ContactFirstName }
    if ($ContactLastName)  { $newContactParams["LastName"]  = $ContactLastName }

    $newContact = New-MailContact @newContactParams

    # --- Донастройка контакта ---
    $setContactParams = @{ Identity = $newContact.Identity }
    if ($ContactCompany)    { $setContactParams["Company"]    = $ContactCompany }
    if ($ContactDepartment) { $setContactParams["Department"] = $ContactDepartment }
    if ($ContactTitle)      { $setContactParams["Title"]      = $ContactTitle }
    if ($ContactPhone)      { $setContactParams["Phone"]      = $ContactPhone }

    if ($setContactParams.Count -gt 1) {
        Set-Contact @setContactParams
    }

    Set-MailContact -Identity $newContact.Identity -HiddenFromAddressListsEnabled $HideFromGAL

    $result.Success         = $true
    $result.Message         = "MailContact '$ContactName' успешно создан в '$ContactOU' и виден в GAL (HiddenFromAddressListsEnabled=$HideFromGAL)."
    $result.ContactIdentity = $newContact.Identity.ToString()

    Write-Result $result
    exit 0
}
catch {
    $result.Success = $false
    $result.Message = "Ошибка при создании MailContact"
    $result.Error   = $_.Exception.Message

    Write-Result $result
    Write-Error $_.Exception.Message
    exit 1
}
finally {
    if ($ExchangeSession) {
        Remove-PSSession $ExchangeSession -ErrorAction SilentlyContinue
    }
}