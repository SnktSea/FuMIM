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
        listOf("userPrincipalName", "uSNChanged"))

    val users = mutableListOf<UserShort>()
    result.searchEntries.forEach { e ->
        users.add(
            UserShort(
                userPrincipalName = e.getAttributeValue("userPrincipalName"),
                userHash = e.getAttributeValue("uSNChanged"),
            )
        )
    }
    return users
}

fun collectUsers(
    server: Server,
    maxAttempts: Int,
    userList: List<UserShort>
): List<User> {
    val filterExpr = combineExpressions(
        processToExpression(server.filters),
        generateExpressionFromUserPrincipals(userList.map { it.userPrincipalName }),
        '&'
    )
    val result = getUserAttributesWithFilter(
        server,
        maxAttempts,
        filterExpr,
        listOf("userPrincipalName", "uSNChanged")) //TODO а что нам вообще нужно?

    val users = mutableListOf<User>()
    result.searchEntries.forEach { e ->
        users.add(
            User(
                userPrincipalName = e.getAttributeValue("userPrincipalName"),
                userHash = e.getAttributeValue("uSNChanged"),
            )
        )
    }
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

    logger.debug { "(1/2) User fetching..." }
    var result: SearchResult?
    for (attempt in 1..maxAttempts) {
        try {
            result = conn.search(query)
            logger.debug { "(1/2) Fetching completed." }
            return result
        } catch (e: Exception) {
            if (attempt == maxAttempts) {
                throw RuntimeException("(1/2) Failed to fetch users after $attempt attempts", e)
            }
            logger.warn(e) { "(1/2) Error fetching users attempt $attempt/$maxAttempts" }
            Thread.sleep(attempt * 1000L)
        } finally {
            conn.close()
        }
    }
    error("Unreachable code")
}