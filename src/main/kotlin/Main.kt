package snkt.org

import io.github.oshai.kotlinlogging.KotlinLogging
import snkt.org.ADIntegraion.collectObjects
import snkt.org.DB.*
import snkt.org.model.Group
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
    getConnection() // Создание соединения с БД (Прогрев)

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
    val groupsToDelete = mutableListOf<String>()
    appConf.servers.forEach {
        logger.info { "Processing ${it.domain}" }
        usersToDelete.addAll(userProcessor(appConf.config, it))
        groupsToDelete.addAll(groupProcessor(it, appConf.config))
    }
    exportListToCsv(usersToDelete, String::class, appConf.config.usersToDeleteCsvPath)
    exportListToCsv(groupsToDelete, String::class, appConf.config.groupsToDeleteCsvPath)

    val usersToExport = fetchAllNotExportedObjects(
        User::class,
        "users",
        listOf("userPrincipal", "userHash", "displayName", "email", "firstName", "lastName")
    )
    val groupsToExport = fetchAllNotExportedObjects(
        Group::class,
        "groups",
        listOf("mail", "groupHash", "displayName", "mailNickname", "proxyAddresses")
    )
    exportListToCsv(usersToExport, User::class, appConf.config.usersToAddPath)
    exportListToCsv(groupsToExport, Group::class, appConf.config.groupsToAddPath)
    updateExportMarkForUsers(usersToExport)

    getConnection().close()
    logger.debug { "DB connection successfully closed" }
    logger.info { "Shutting down. Bye!" }
}