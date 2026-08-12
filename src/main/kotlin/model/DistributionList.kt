package snkt.org.model

data class GroupShort(
    val mail: String,
    val groupHash: String
)

data class Group(
    val mail: String,
    val groupHash: String,
    val displayName: String,
    val mailNickname: String,
    val proxyAddresses: String,
)