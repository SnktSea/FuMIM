package snkt.org

import io.github.oshai.kotlinlogging.KotlinLogging
import snkt.org.ADIntegraion.collectAdShortUsers
import snkt.org.ADIntegraion.collectUsers
import snkt.org.DB.deleteUsers
import snkt.org.DB.initializeDb
import snkt.org.DB.insertUsersAndRewriteOld
import snkt.org.model.helpText
import java.nio.file.Files
import kotlin.io.path.Path

val logger = KotlinLogging.logger {}
val confFile: String = Files.readString(Path("conf.toml"))

fun main(args: Array<String>) {
    logger.info { "Stating application..." }

    var optind = 0
    while (optind < args.size) {
        if (args[optind] == "--help") {
            println(helpText)
        } else if (args[optind] == "--init-db") {
            logger.info { "Initializing DB..." }
            initializeDb()
        }
        optind++
    }
    val appConf = parseConf(confFile)
    appConf.servers.forEach {
        val adUsers = collectAdShortUsers(
            it,
            appConf.config.maxAttempts
        )
        val (cleanList, pullList) = compareAdUsersToLocal(
            "",
            adUsers
        )
        deleteUsers(cleanList)
        val usersToWrite = collectUsers(
            it,
            appConf.config.maxAttempts,
            pullList
        )
        insertUsersAndRewriteOld(usersToWrite)
    }
}