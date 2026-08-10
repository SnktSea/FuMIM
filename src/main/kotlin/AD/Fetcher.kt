package snkt.org.ADIntegraion

import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import snkt.org.logger
import snkt.org.model.Server
import snkt.org.model.User
import snkt.org.model.UserShort


fun collectAdShortUsers(
    server: Server,
    maxAttempts: Int
): List<UserShort> {
    val result = getUserAttributesWithFilter(
        server,
        maxAttempts,
        processToExpression(server.filters),
        listOf("userPrincipalName", "uSNChanged")
    )

    logger.debug { "Collecting short users... batch size: ${result.searchEntries.size}" }
    val users = mutableListOf<UserShort>()
    result.searchEntries
        .filter { e ->
            e.getAttributeValue("userPrincipalName") != null &&
                    e.getAttributeValue("uSNChanged") != null
        }
        .forEach { e ->
            users.add(
                UserShort(
                    userPrincipalName = e.getAttributeValue("userPrincipalName"),
                    userHash = e.getAttributeValue("uSNChanged"),
                )
            )
        }
    logger.debug { "Short users collected: ${users.size}" }
    return users
}

fun collectUsers(
    server: Server,
    maxAttempts: Int,
    userList: List<UserShort>
): List<User> {
    if (userList.isEmpty()) {
        return listOf()
    }

    val filterExpr = combineExpressions(
        processToExpression(server.filters),
        generateExpressionFromUserPrincipals(userList.map { it.userPrincipalName }),
        '&'
    )
    val result = getUserAttributesWithFilter(
        server,
        maxAttempts,
        filterExpr,
        listOf("userPrincipalName", "uSNChanged", "displayName", "mail", "givenName", "sn")
    )

    logger.debug { "Collecting users... batch size: ${result.searchEntries.size}" }
    val users = mutableListOf<User>()
    result.searchEntries
//        .filter { e ->
//            e.getAttributeValue("userPrincipalName") != null &&
//                    e.getAttributeValue("uSNChanged") != null &&
//                    e.getAttributeValue("displayName") != null &&
//                    e.getAttributeValue("mail") != null &&
//                    e.getAttributeValue("givenName") != null &&
//                    e.getAttributeValue("sn") != null
//        }
        .forEach { e ->
            val upn = e.getAttributeValue("userPrincipalName")
            val usn = e.getAttributeValue("uSNChanged")
            val disp = e.getAttributeValue("displayName")
            val mail = e.getAttributeValue("mail")
            val given = e.getAttributeValue("givenName")
            val sur = e.getAttributeValue("sn")

            if (upn == null || usn == null || disp == null || mail == null || given == null || sur == null) {
                logger.debug { "User skipped. UPN:$upn, USN:$usn, DisplayName:$disp, Mail:$mail, Given:$given, Surname:$sur" }
            } else {
                users.add(User(upn, usn, disp, mail, given, sur))
            }
    }
    logger.debug { "Users collected: ${users.size}" }
    return users
}

fun getUserAttributesWithFilter(
    server: Server,
    maxAttempts: Int,
    filterExpr: String,
    attributes: List<String>
): SearchResult {
    val conn = createLDAPConnection(
        server.host,
        server.port,
        server.user,
        server.password,
        maxAttempts
    )
    val filter = Filter.create(filterExpr)
    val query = SearchRequest(
        server.userPath,
        SearchScope.SUB,
        filter,
        *attributes.toTypedArray()
    )

    logger.debug { "User fetching..." }
    var result: SearchResult?
    for (attempt in 1..maxAttempts) {
        try {
            result = conn.search(query)
            logger.debug { "Fetching completed." }
            return result
        } catch (e: Exception) {
            if (attempt == maxAttempts) {
                throw RuntimeException("Failed to fetch users after $attempt attempts", e)
            }
            logger.warn(e) { "Error fetching users attempt $attempt/$maxAttempts" }
            Thread.sleep(attempt * 1000L)
        } finally {
            conn.close()
        }
    }
    error("Unreachable code")
}