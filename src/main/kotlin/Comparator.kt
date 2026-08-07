package snkt.org

import snkt.org.DB.fetchUsersByDomain
import snkt.org.model.UserShort

fun compareAdUsersToLocal(domain: String, adUsers: List<UserShort>): Pair<List<String>, List<UserShort>> {
    val localUsers = fetchUsersByDomain(domain)

    val localUserSet = localUsers.toSet()
    val adUserSet = adUsers.toSet()

    val cleanList = (localUserSet - adUserSet).map { it.userPrincipalName }
    val pullList = (adUserSet - localUserSet).toList()
    return cleanList to pullList
}