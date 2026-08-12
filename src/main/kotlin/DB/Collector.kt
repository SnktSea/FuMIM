package snkt.org.DB

import snkt.org.logger
import snkt.org.model.GroupShort
import snkt.org.model.User
import snkt.org.model.UserShort
import kotlin.reflect.KClass

fun fetchShortUsersByDomain(domain: String): List<UserShort> {
    val selectUsers = "SELECT userPrincipal, userHash FROM users WHERE domain = ?"
    getConnection().prepareStatement(selectUsers).use { pstmt ->
        pstmt.setString(1, domain)
        val resultSet = pstmt.executeQuery()

        logger.debug { "Collecting short users by domain ($domain) from local storage" }
        val users = mutableListOf<UserShort>()
        while (resultSet.next()) {
            users.add(
                UserShort(
                    userPrincipalName = resultSet.getString("userPrincipal"),
                    userHash = resultSet.getString("userHash")
                )
            )
        }
        return users
    }
}

fun fetchShortGroupsByDomain(domain: String): List<GroupShort> {
    val selectGroups = "SELECT mail, groupHash FROM groups WHERE domain = ?"
    getConnection().prepareStatement(selectGroups).use { pstmt ->
        pstmt.setString(1, domain)
        val resultSet = pstmt.executeQuery()

        logger.debug { "Collecting short groups by domain ($domain) from local storage" }
        val groups = mutableListOf<GroupShort>()
        while (resultSet.next()) {
            groups.add(
                GroupShort(
                    mail = resultSet.getString("mail"),
                    groupHash = resultSet.getString("groupHash")
                )
            )
        }
        return groups
    }
}

fun fetchUsersNotInDomainAndNotExported(domain: String): List<User> {
    val selectUsers = "SELECT * FROM users WHERE domain != ? AND exported = false"
    getConnection().prepareStatement(selectUsers).use { pstmt ->
        pstmt.setString(1, domain)
        val resultSet = pstmt.executeQuery()

        logger.debug { "Collecting short users not in $domain and not exported from local storage" }
        val users = mutableListOf<User>()
        while (resultSet.next()) {
            users.add(
                User(
                    userPrincipalName = resultSet.getString("userPrincipal"),
                    userHash = resultSet.getString("userHash"),
                    displayName = resultSet.getString("displayName"),
                    email = resultSet.getString("email"),
                    firstName = resultSet.getString("firstName"),
                    lastName = resultSet.getString("lastName")
                )
            )
        }
        return users
    }
}

fun <T : Any> fetchAllNotExportedObjects(
    targetClass: KClass<T>,
    tableName: String,
    attributes: List<String>
): List<T> {
    val selectObjects = "SELECT * FROM $tableName WHERE exported = false"
    getConnection().prepareStatement(selectObjects).use { pstmt ->
        val resultSet = pstmt.executeQuery()

        logger.debug { "Collecting not exported objects from local storage" }
        val objects = mutableListOf<T>()
        while (resultSet.next()) {
            val constructorArgs = attributes.map { attr -> resultSet.getString(attr) }.toTypedArray()
            val constructor = targetClass.java.constructors.firstOrNull { it.parameterCount == constructorArgs.size }

            objects.add(
                (constructor?.newInstance(*constructorArgs)
                    ?: IllegalStateException("${targetClass.simpleName} does not have constructor parameters")
                ) as T
            )
        }
        return objects
    }
}