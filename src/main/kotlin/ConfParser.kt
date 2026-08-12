package snkt.org

import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.TomlTable
import net.peanuuutz.tomlkt.asTomlLiteral
import net.peanuuutz.tomlkt.asTomlTable
import net.peanuuutz.tomlkt.decodeFromString
import snkt.org.model.AppConf
import snkt.org.model.Filter
import snkt.org.model.Server

fun parseConf(conf: String): AppConf {
    val table = Toml.decodeFromString(TomlTable.serializer(), conf)
    logger.debug { table }

    val toml = Toml {
        ignoreUnknownKeys = true
    }
    val tomlConf = toml.decodeFromString<TomlConf>(conf)

    val servers = mutableListOf<Server>()
    tomlConf.servers.forEach { (key, value) ->
        try {
            val serverTable = table[key]?.asTomlTable()
                ?: throw IllegalStateException("Table '$key' is missing or is not a TOML table")
            val serverContainsRulesTable = serverTable["userContains"]?.asTomlTable()
            if (serverContainsRulesTable?.isEmpty() ?: true) logger.warn { "You haven't defined '+' rule set for $key" }
            val serverDoesntContainsRulesTable = serverTable["userDoesntContains"]?.asTomlTable()
            if (serverDoesntContainsRulesTable?.isEmpty() ?: true) logger.warn { "You haven't defined '-' rule set for $key" }

            val filters = mutableListOf<Filter>()
            serverContainsRulesTable?.forEach { (key, value) -> filters
                .add(
                    Filter(
                        filterType = '+',
                        key = key,
                        value = value.asTomlLiteral().content
                    )
                )
            }
            serverDoesntContainsRulesTable?.forEach { (key, value) -> filters
                .add(
                    Filter(
                        filterType = '-',
                        key = key,
                        value = value.asTomlLiteral().content
                    )
                )}

            servers.add(Server(
                domain = serverTable["domain"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'domain' in server '$key'"),
                host = value.asTomlLiteral().toString(),
                user = serverTable["user"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'user' in server '$key'"),
                password = serverTable["password"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'password' in server '$key'"),
                port = serverTable["port"]?.asTomlLiteral()?.content?.toInt()
                    ?: throw IllegalStateException("Missing 'port' in server '$key'"),
                userPath = serverTable["userPath"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'userPath' in server '$key'"),
                groupPath = serverTable["groupPath"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'userPath' in server '$key'"),
                filters = filters,
                exchangeHost = serverTable["exchangeHost"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'userPath' in server '$key'"),
                skipCertificateCheck = serverTable["skipCertificateCheck"]?.asTomlLiteral()?.content?.toBoolean()
                    ?: throw IllegalStateException("Missing 'skipCertificateCheck' in server '$key'"),
                exchangeAuthType = serverTable["exchangeAuthType"]?.asTomlLiteral()?.content
                    ?: throw IllegalStateException("Missing 'exchangeAuthType' in server '$key'"),
            )
            )
        } catch (e: Exception) {
            logger.error { "Bad configuration file: key: $key, value: $value" }
            logger.error { e.stackTraceToString() }
        }
    }
    return AppConf(servers, tomlConf.config)
}

@Serializable
data class TomlConf(
    val config: Config,
    val servers: TomlTable
)

@Serializable
data class Config(
    val maxAttempts: Int,
    val usersToDeleteCsvPath: String,
    val usersToAddPath: String,
    val groupsToDeleteCsvPath: String,
    val groupsToAddPath: String
)