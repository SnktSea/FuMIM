import io.github.serpro69.kfaker.Faker
import org.junit.jupiter.api.Test
import snkt.org.DB.deleteUsers
import snkt.org.DB.fetchShortUsersByDomain
import snkt.org.DB.getConnection
import snkt.org.DB.insertUsersAndRewriteOld
import snkt.org.model.User
import snkt.org.model.UserShort
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.random.Random

val faker = Faker()

class TestSqlite {

    @Test
    fun `Test reading, writing and deleting objects`() {
        val userList = List(100) {
            UserShort(
                userPrincipalName = faker.internet.email(),
                userHash = faker.crypto.md5()
            )
        }

        getConnection().use { conn ->

            // Preparing
            conn.prepareStatement("DELETE FROM users").use { it.executeUpdate() }

            // Writing
//            insertUsersAndRewriteOld(userList, "snkt.test")

            // Reading
            var counter = 0
            conn.prepareStatement("SELECT * FROM users").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        println("${counter++}: ${resultSet.getString(1)} ${resultSet.getString(2)}")
                    }
                }
            }

            // Deleting
            deleteUsers(userList.map { it.userPrincipalName })

            expectThat(counter).isEqualTo(100)
            expectThat(
                conn
                    .prepareStatement(
                        "SELECT count() FROM users"
                    ).use {
                        it.executeQuery().use {
                            it.next()
                            it.getInt(1)
                        }
                    }
            ).isEqualTo(0)
        }
    }

    @Test
    fun `Test fetching values by domain`() {
        val userList = List(100) {
            UserShort(
                userPrincipalName = faker.internet.email(),
                userHash = faker.crypto.md5()
            )
        }
        val domains = listOf("snkt.dev", "snkt.prod", "snkt.test")
        val domainCounters = mutableListOf(0, 0, 0)

        getConnection().use { conn ->

            // Preparing
            conn.prepareStatement("DELETE FROM users").use { it.executeUpdate() }

            // Writing
            val query = """
                INSERT OR REPLACE INTO users (userPrincipal, userHash, domain) VALUES (?, ?, ?)
            """.trimIndent()

            conn.autoCommit = false
            try {
                conn.prepareStatement(query).use { stmt ->
                    for (user in userList) {
                        val selectedDomain = Random.nextInt(0, 3)
                        stmt.setString(1, user.userPrincipalName)
                        stmt.setString(2, user.userHash)
                        stmt.setString(3, domains[selectedDomain])
                        domainCounters[selectedDomain]++

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
            for (i in domains.indices) {
                expectThat(fetchShortUsersByDomain(domains[i]).size).isEqualTo(domainCounters[i])
            }

            // Cleaning
            conn.prepareStatement("DELETE FROM users").use { it.executeUpdate() }
        }
    }
}