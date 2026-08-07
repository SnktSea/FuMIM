package snkt.org.model

data class Server(
    val domain: String,
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val userPath: String,
    val filters: List<Filter>
)
