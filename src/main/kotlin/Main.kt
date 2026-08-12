package snkt.org

import io.github.oshai.kotlinlogging.KotlinLogging
import snkt.org.ADIntegraion.collectAdShortUsers
import snkt.org.ADIntegraion.collectUsers
import snkt.org.DB.deleteUsers
import snkt.org.DB.fetchAllNotExportedUsers
import snkt.org.DB.fetchShortUsersByDomain
import snkt.org.DB.fetchUsersNotInDomainAndNotExported
import snkt.org.DB.getConnection
import snkt.org.DB.initializeDb
import snkt.org.DB.insertUsersAndRewriteOld
import snkt.org.DB.updateExportMarkForUsers
import snkt.org.model.User
import snkt.org.model.UserShort
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
    getConnection() // Создание соединения с БД

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

    val usersToDelete = mutableListOf<String>()
    appConf.servers.forEach {
        logger.info { "Processing ${it.domain}" }
        val adUsers = collectAdShortUsers(
            it,
            appConf.config.maxAttempts
        )
        val (cleanList, pullList) = compareAdUsersToLocal(
            it.domain,
            adUsers,
            fetchShortUsersByDomain(it.domain)
        )
        usersToDelete.addAll(cleanList)
        deleteUsers(cleanList)

        val usersToWrite = collectUsers(
            it,
            appConf.config.maxAttempts,
            pullList
        )
        insertUsersAndRewriteOld(usersToWrite, it.domain)
    }
    exportListToCsv(usersToDelete, String::class, appConf.config.usersToDeleteCsvPath)

    val usersToExport = fetchAllNotExportedUsers()
    exportListToCsv(usersToExport, User::class, appConf.config.usersToAddPath)
    updateExportMarkForUsers(usersToExport)

    getConnection().close()
    logger.debug { "DB connection successfully closed" }
    logger.info { "Shutting down. Bye!" }
}