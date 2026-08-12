package snkt.org.ADIntegraion

import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.SearchRequest
import com.unboundid.ldap.sdk.SearchResult
import com.unboundid.ldap.sdk.SearchScope
import snkt.org.logger
import snkt.org.model.Server
import kotlin.reflect.KClass


fun <T : Any> collectObjects(
    server: Server,
    maxAttempts: Int,
    attributes: List<String>,
    path: String,
    targetClass: KClass<T>,
    pullList: List<String>? = null,
    filterKey: String? = null,
): List<T> {
    if (pullList != null && pullList.isEmpty()) {
        return emptyList()
    }
    if (pullList != null && filterKey == null) {
        throw IllegalStateException("'filterKey' argument is empty")
    }

    val filterExpr = if (pullList != null)
        combineExpressions(
        processToExpression(server.filters),
        generateExpressionFromUserPrincipals(
            pullList,
            filterKey!!
        ),
        '&'
    ) else processToExpression(server.filters)

    val result = getObjectAttributesWithFilter(
        server,
        maxAttempts,
        filterExpr,
        attributes,
        path
    )

    logger.debug { "Collecting short ${targetClass.simpleName}... batch size: ${result.searchEntries.size}" }
    val objects = mutableListOf<T>()
    result.searchEntries
        .filter { e ->
            attributes.all { attr -> e.getAttributeValue(attr) != null }
        }
        .forEach { e ->
            val constructorArgs = attributes.map { attr -> e.getAttributeValue(attr) }.toTypedArray()
            val constructor = targetClass.java.constructors.firstOrNull { it.parameterCount == constructorArgs.size }

            objects.add(
                (constructor?.newInstance(*constructorArgs)
                    ?: IllegalStateException("${targetClass.simpleName} does not have constructor parameters")
                        ) as T
            )
        }
    logger.debug { "Short objects collected: ${objects.size}" }
    return objects
}

fun getObjectAttributesWithFilter(
    server: Server,
    maxAttempts: Int,
    filterExpr: String,
    attributes: List<String>,
    path: String
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
        path,
        SearchScope.SUB,
        filter,
        *attributes.toTypedArray()
    )

    logger.debug { "Object fetching..." }
    var result: SearchResult?
    for (attempt in 1..maxAttempts) {
        try {
            result = conn.search(query)
            logger.debug { "Fetching completed." }
            return result
        } catch (e: Exception) {
            if (attempt == maxAttempts) {
                throw RuntimeException("Failed to fetch objects after $attempt attempts", e)
            }
            logger.warn(e) { "Error fetching objects attempt $attempt/$maxAttempts" }
            Thread.sleep(attempt * 1000L)
        } finally {
            conn.close()
        }
    }
    error("Unreachable code")
}