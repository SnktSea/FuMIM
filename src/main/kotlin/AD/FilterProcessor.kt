package snkt.org.ADIntegraion

import snkt.org.logger
import snkt.org.model.Filter


fun processToExpression(filters: List<Filter>): String {
    logger.debug { "Processing filters..." }
    if (filters.isEmpty()) {
        logger.debug { "No filters found" }
        return ""
    } else {
        val sb = StringBuilder().append("(&") //TODO оператор &?
        filters.forEach { filter ->
            val isNegative = filter.filterType == '-'
            val prefix = if (isNegative) "(!" else ""
            val suffix = if (isNegative) ")" else ""

            sb.append(
                "$prefix(${filter.key}=${filter.value})$suffix"
            )
        }
        sb.append(")")
        logger.debug { "Filter expression: $sb" }
        return sb.toString()
    }
}

fun generateExpressionFromUserPrincipals(users: List<String>): String {
    logger.debug { "Generating filter from user principals..." }
    if (users.isEmpty()) {
        logger.debug { "No filters found" }
        return ""
    } else {
        val sb = StringBuilder()
        sb.append("(|")
        users.forEach { user ->
            sb.append("(userPrincipalName=$user)")
        }
        sb.append(")")
        logger.debug { "Filter expression: $sb" }
        return sb.toString()
    }
}

fun combineExpressions(expr1: String, expr2: String, operator: Char): String {
    return "($operator$expr1$expr2)"
}