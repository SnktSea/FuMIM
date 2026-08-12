package snkt.org

import snkt.org.ADIntegraion.collectObjects
import snkt.org.DB.deleteUsers
import snkt.org.DB.fetchShortUsersByDomain
import snkt.org.DB.insertUsersAndRewriteOld
import snkt.org.model.Server
import snkt.org.model.User
import snkt.org.model.UserShort

fun userProcessor(config: Config, server: Server): List<String> {
    val usersToDelete = mutableListOf<String>()
    val adUsers = collectObjects(
        server,
        config.maxAttempts,
        listOf("userPrincipalName", "uSNChanged"),
        server.userPath,
        UserShort::class
    )
    val (cleanList, pullList) = compareADObjectsToLocal(
        server.domain,
        adUsers,
        fetchShortUsersByDomain(server.domain)
    )
    val mappedCleanList = cleanList.map { it.userPrincipalName }
    usersToDelete.addAll(mappedCleanList)
    deleteUsers(mappedCleanList)

    val usersToWrite = collectObjects(
        server = server,
        maxAttempts = config.maxAttempts,
        attributes = listOf("userPrincipalName", "uSNChanged", "displayName", "mail", "givenName", "sn"),
        path = server.userPath,
        targetClass = User::class,
        pullList = pullList.map { it.userPrincipalName },
        filterKey = "userPrincipalName"
    )
    insertUsersAndRewriteOld(usersToWrite, server.domain)
    return usersToDelete
}
