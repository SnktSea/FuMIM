package snkt.org.DB

import snkt.org.logger
import snkt.org.model.Group
import snkt.org.model.User

fun insertUsersAndRewriteOld(usersToWrite: List<User>, domain: String) {
    if (usersToWrite.isEmpty()) {
        return
    }

    val query = """
            INSERT OR REPLACE INTO users (userPrincipal, userHash, domain, displayName, email, firstName, lastName) VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

    getConnection().autoCommit = false
    try {
        getConnection().prepareStatement(query).use { stmt ->
            for (user in usersToWrite) {
                stmt.setString(1, user.userPrincipalName)
                stmt.setString(2, user.userHash)
                stmt.setString(3, domain)
                stmt.setString(4, user.displayName)
                stmt.setString(5, user.email)
                stmt.setString(6, user.firstName)
                stmt.setString(7, user.lastName)

                stmt.addBatch()
            }
            logger.debug { "Adding new users to storage: ${usersToWrite.size}" }
            stmt.executeBatch()
        }
        getConnection().commit()
    } catch (ex: Exception) {
        getConnection().rollback()
        throw ex
    } finally {
        getConnection().autoCommit = true
    }
}

fun insertGroupsAndRewriteOld(groupsToWrite: List<Group>, domain: String) {
    if (groupsToWrite.isEmpty()) {
        return
    }

    val query = """
            INSERT OR REPLACE INTO groups (mail, groupHash, displayName, mailNickname, proxyAddresses) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

    getConnection().autoCommit = false
    try {
        getConnection().prepareStatement(query).use { stmt ->
            for (group in groupsToWrite) {
                stmt.setString(1, group.mail)
                stmt.setString(2, group.groupHash)
                stmt.setString(3, group.displayName)
                stmt.setString(4, group.mailNickname)
                stmt.setString(5, group.proxyAddresses)

                stmt.addBatch()
            }
            logger.debug { "Adding new groups to storage: ${groupsToWrite.size}" }
            stmt.executeBatch()
        }
        getConnection().commit()
    } catch (ex: Exception) {
        getConnection().rollback()
        throw ex
    } finally {
        getConnection().autoCommit = true
    }
}

fun deleteUsers(usersToDelete: List<String>) {
    if (usersToDelete.isEmpty()) {
        return
    }

    val placeholders = usersToDelete.joinToString(",") { "?" }
    val query = """
            DELETE FROM users WHERE userPrincipal IN ($placeholders)
        """.trimIndent()

    getConnection().prepareStatement(query).use { pstmt ->

        usersToDelete.forEachIndexed { index, string ->
            pstmt.setString(index + 1, string)
        }
        logger.debug { "Deleting users from storage: ${usersToDelete.size}" }
        pstmt.executeUpdate()
    }
}

fun deleteGroups(usersToDelete: List<String>) {
    if (usersToDelete.isEmpty()) {
        return
    }

    val placeholders = usersToDelete.joinToString(",") { "?" }
    val query = """
            DELETE FROM groups WHERE mail IN ($placeholders)
        """.trimIndent()

    getConnection().prepareStatement(query).use { pstmt ->

        usersToDelete.forEachIndexed { index, string ->
            pstmt.setString(index + 1, string)
        }
        logger.debug { "Deleting groups from storage: ${usersToDelete.size}" }
        pstmt.executeUpdate()
    }
}

fun updateExportMarkForObjects(objects: List<String>, tableName: String, idColumn: String) {
    if (objects.isEmpty()) {
        return
    }

    val query = """
        UPDATE $tableName SET exported = true WHERE $idColumn = ?
        """.trimIndent()
    getConnection().autoCommit = false
    try {
        getConnection().prepareStatement(query).use { stmt ->
            for (o in objects) {
                stmt.setString(1, o)
                stmt.addBatch()
            }
            logger.debug { "Marking objects as exported in local storage: ${objects.size}" }
            stmt.executeBatch()
        }
        getConnection().commit()
    } catch (ex: Exception) {
        getConnection().rollback()
        throw ex
    } finally {
        getConnection().autoCommit = true
    }
}