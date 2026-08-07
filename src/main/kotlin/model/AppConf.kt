package snkt.org.model

import snkt.org.Config

data class AppConf(
    val servers: List<Server>,
    val config: Config
)
