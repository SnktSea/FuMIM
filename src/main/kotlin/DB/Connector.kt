package snkt.org.DB

import java.sql.Connection
import java.sql.DriverManager

const val url = "jdbc:sqlite:fumim.db"

fun getConnection(): Connection {
    return DriverManager.getConnection(url)
}