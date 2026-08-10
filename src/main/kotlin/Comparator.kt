package snkt.org

import snkt.org.DB.fetchShortUsersByDomain
import snkt.org.model.UserShort

fun compareAdUsersToLocal(domain: String, adUsers: List<UserShort>): Pair<List<String>, List<UserShort>> {
    val localUsers = fetchShortUsersByDomain(domain)

    val localUserSet = localUsers.toSet()
    val adUserSet = adUsers.toSet()

    val cleanList = (localUserSet - adUserSet).map { it.userPrincipalName }
    logger.debug { "Amount users to delete from local storage ${cleanList.size}" }
    val pullList = (adUserSet - localUserSet).toList()
    logger.debug { adUserSet.toString() }
    logger.debug { "Amount of users to pull from AD $domain: ${pullList.size}" }

    logger.debug { "Pull list: $pullList" }
    logger.debug { "Clean list: $cleanList" }
    return cleanList to pullList
}