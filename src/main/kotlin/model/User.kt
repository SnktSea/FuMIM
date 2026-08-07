package snkt.org.model

data class UserShort(
    val userPrincipalName: String,
    val userHash: String,
)

data class User(
    val userPrincipalName: String,
    val userHash: String,
)