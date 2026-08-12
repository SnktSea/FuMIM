package snkt.org

import snkt.org.ADIntegraion.collectObjects
import snkt.org.DB.deleteGroups
import snkt.org.DB.deleteUsers
import snkt.org.DB.fetchShortGroupsByDomain
import snkt.org.DB.insertGroupsAndRewriteOld
import snkt.org.DB.insertUsersAndRewriteOld
import snkt.org.model.Group
import snkt.org.model.GroupShort
import snkt.org.model.Server
import snkt.org.model.User

fun groupProcessor(server: Server, config: Config): List<String> {
    val groupToDelete = mutableListOf<String>()
    val adGroups = collectObjects(
        server,
        config.maxAttempts,
        listOf("mail", "uSNChanged"),
        server.groupPath,
        GroupShort::class
    )
    val (cleanList, pullList) = compareADObjectsToLocal(
        server.domain,
        adGroups,
        fetchShortGroupsByDomain(server.domain)
    )
    val mappedCleanList = cleanList.map { it.mail }

    groupToDelete.addAll(mappedCleanList)
    deleteGroups(mappedCleanList)

    val groupsToWrite = collectObjects(
        server = server,
        maxAttempts = config.maxAttempts,
        attributes = listOf("mail", "uSNChanged", "displayName", "mailNickname", "proxyAddresses"),
        path = server.groupPath,
        targetClass = Group::class,
        pullList = pullList.map { it.mail },
        filterKey = "userPrincipalName"
    )
    logger.debug { "Groups to write size: ${groupsToWrite.size}" }
    insertGroupsAndRewriteOld(groupsToWrite, server.domain)
    return groupToDelete
}