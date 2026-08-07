package snkt.org.DB

import snkt.org.model.UserShort

fun fetchUsersByDomain(domain: String): List<UserShort> {
    getConnection().use { conn ->
        val selectUsers = "SELECT userPrincipal, userHash FROM users u WHERE u.domain = ?"
        conn.prepareStatement(selectUsers).use { pstmt ->
            pstmt.setString(1, domain)
            val resultSet = pstmt.executeQuery()

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
}