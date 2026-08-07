package snkt.org.DB

import snkt.org.model.User
import snkt.org.model.UserShort

fun insertUsersAndRewriteOld(usersToWrite: List<User>, domain: String) {
    if (usersToWrite.isEmpty()) {
        return
    }

    getConnection().use { conn ->
        val query = """
            INSERT OR REPLACE INTO users (userPrincipal, userHash, domain) VALUES (?, ?, ?)
        """.trimIndent()

        conn.autoCommit = false
        try {
            conn.prepareStatement(query).use { stmt ->
                for (user in usersToWrite) {
                    stmt.setString(1, user.userPrincipalName)
                    stmt.setString(2, user.userHash)
                    stmt.setString(3, domain)

                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
            conn.commit()
        } catch (ex: Exception) {
            conn.rollback()
            throw ex
        } finally {
            conn.autoCommit = true
        }
    }
}

fun deleteUsers(usersToDelete: List<String>) {
    if (usersToDelete.isEmpty()) {
        return
    }

    getConnection().use { conn ->
        val placeholders = usersToDelete.joinToString(",") { "?" }
        val query = """
            DELETE FROM users WHERE userPrincipal IN ($placeholders)
        """.trimIndent()

        conn.prepareStatement(query).use { pstmt ->

            usersToDelete.forEachIndexed { index, string ->
                pstmt.setString(index + 1, string)
            }
            pstmt.executeUpdate()
        }
    }
}