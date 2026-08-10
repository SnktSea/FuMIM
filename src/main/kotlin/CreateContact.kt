package snkt.org

import kotlin.io.path.Path
import kotlin.io.path.absolute

fun createContactAndExportToAD(
    exchangeHost: String,
    exchangeUri: String?,
    exchangeUser: String,
    exchangePassword: String,
    exchangeAuthType: String,
    skipCertificateCheck: Boolean = false,
    contactName: String,
    firstName: String,
    lastName: String,
    contactAlias: String,
    externalEmail: String,
    targetOu: String,
) {
    val scriptPath = Path("create_contact.ps1").absolute().toString()
    logger.debug { "'create_contact.ps1' script path: $scriptPath" }

    val command = listOf(
        "powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", scriptPath,
        "-ExchangeServerFQDN", exchangeHost,
        if (exchangeUri != null) "-ExchangeConnectionUri" else "", exchangeUri ?: "",
        "-ExchangeUser", exchangeUser,
        "-ExchangePassword", exchangePassword,
        "-ExchangeAuthType", exchangeAuthType,
        if (skipCertificateCheck) "-SkipCertificateCheck" else "",
        "-ContactName", contactName,
        "-ContactAlias", contactAlias,
        "-ContactFirstName", firstName,
        "-ContactLastName", lastName,
        "-ContactExternalEmail", externalEmail,
        "-ContactOU", targetOu
    )

    logger.debug { "Creating contact for $contactName" }
    val process = ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()

    logger.debug { "Exit code: $exitCode" }
    if (exitCode != 0) {
        logger.error { "Exit output: $output" }
    }
}