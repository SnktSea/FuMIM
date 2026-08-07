package snkt.org.DB

fun initializeDb() {
    getConnection().use { conn ->
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                userPrincipal TEXT PRIMARY KEY,
                userHash TEXT NOT NULL,
                domain TEXT NOT NULL
                )
            """.trimIndent())
        }
    }
}