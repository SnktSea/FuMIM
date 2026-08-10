package snkt.org

import io.github.oshai.kotlinlogging.KotlinLogging
import snkt.org.ADIntegraion.collectAdShortUsers
import snkt.org.ADIntegraion.collectUsers
import snkt.org.DB.deleteUsers
import snkt.org.DB.fetchUsersNotInDomainAndNotExported
import snkt.org.DB.getConnection
import snkt.org.DB.initializeDb
import snkt.org.DB.insertUsersAndRewriteOld
import snkt.org.DB.updateExportMarkForUsers
import snkt.org.model.helpText
import java.io.IOException
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists

val logger = KotlinLogging.logger {}
val confFile: String = Files.readString(
    if (Path("conf.toml").exists()) Path("conf.toml")
    else throw IOException("Failed to read the TOML file. Place it next to the executable file.")
)

fun main(args: Array<String>) {
    logger.info { "Stating application..." }

    var optind = 0
    while (optind < args.size) {
        if (args[optind] == "--help") {
            println(helpText)
            return
        } else if (args[optind] == "--init-db") {
            logger.info { "Initializing DB..." }
            initializeDb()
            logger.info { "Done!" }
            return
        }
        optind++
    }
    val appConf = parseConf(confFile)
    appConf.servers.forEach {
        logger.info { "Processing ${it.domain}" }
        val adUsers = collectAdShortUsers(
            it,
            appConf.config.maxAttempts
        )
        val (cleanList, pullList) = compareAdUsersToLocal(
            it.domain,
            adUsers
        )
        deleteUsers(cleanList)
        val usersToWrite = collectUsers(
            it,
            appConf.config.maxAttempts,
            pullList
        )
        insertUsersAndRewriteOld(usersToWrite, it.domain)
    }
    appConf.servers.forEach { server ->
        logger.info { "Exporting to ${server.domain}" }
        val usersToExport = fetchUsersNotInDomainAndNotExported(server.domain)
        usersToExport.forEach { u ->
            createContactAndExportToAD(
                contactName = u.displayName,
                contactAlias = u.userPrincipalName,
                externalEmail = u.email,
                targetOu = server.userPath,
                exchangeHost = server.exchangeHost,
                exchangeUser = server.user,
                exchangePassword = server.password,
                exchangeAuthType = server.exchangeAuthType,
                skipCertificateCheck = server.skipCertificateCheck,
                firstName = u.displayName,
                lastName = u.displayName,
                exchangeUri = null,
            )
        }
        updateExportMarkForUsers(usersToExport)
    }
    getConnection().close()
    logger.debug { "DB connection successfully closed" }
    logger.info { "Shutting down. Bye!" }
}