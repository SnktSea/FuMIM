package snkt.org.DB

fun initializeDb() {
    getConnection().use { conn ->
        conn.createStatement().use { stmt ->
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                userPrincipal TEXT PRIMARY KEY,
                userHash TEXT NOT NULL,
                domain TEXT NOT NULL,
                displayName TEXT NOT NULL,
                email TEXT NOT NULL,
                firstName TEXT NOT NULL,
                lastName TEXT NOT NULL,
                exported BOOLEAN NOT NULL DEFAULT false
                )
            """.trimIndent())
        }
    }
}