package snkt.org.model

data class Server(
    val domain: String,
    val host: String,
    val port: Int,
    val user: String,
    val password: String,
    val userPath: String,
    val groupPath: String,
    val exchangeHost: String,
    val skipCertificateCheck: Boolean,
    val exchangeAuthType: String,
    val filters: List<Filter>
)
