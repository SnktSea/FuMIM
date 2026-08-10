package snkt.org.DB

import snkt.org.logger
import java.sql.Connection
import java.sql.DriverManager

const val url = "jdbc:sqlite:fumim.db"
private val connection = DriverManager.getConnection(url).also {
    logger.debug { "Creating connection to DB" }
}
fun getConnection(): Connection {
    return connection
}