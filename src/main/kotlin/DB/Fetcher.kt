package snkt.org.DB

import snkt.org.logger
import snkt.org.model.User
import snkt.org.model.UserShort

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

fun fetchAllNotExportedUsers(): List<User> {
    val selectUsers = "SELECT * FROM users WHERE exported = false"
    getConnection().prepareStatement(selectUsers).use { pstmt ->
        val resultSet = pstmt.executeQuery()

        logger.debug { "Collecting not exported short users from local storage" }
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