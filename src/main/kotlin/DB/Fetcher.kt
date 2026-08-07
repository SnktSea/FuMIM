package snkt.org.DB

import snkt.org.model.UserShort

fun fetchUsersByDomain(domain: String): List<UserShort> {
    val resultSet = getConnection().use { conn ->
        val selectUsers = "SELECT (userPrincipal, userHash) FROM users WHERE domain = ?"
        conn.prepareStatement(selectUsers).use { pstmt ->
            pstmt.setString(1, domain)
            pstmt.executeQuery()
        }
    }

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