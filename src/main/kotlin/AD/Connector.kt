package snkt.org.ADIntegraion

import com.unboundid.ldap.sdk.LDAPConnection
import com.unboundid.ldap.sdk.LDAPException
import snkt.org.logger

fun createLDAPConnection(
    host: String,
    port: Int,
    user: String,
    password: String,
    maxAttempts: Int
): LDAPConnection {
    logger.debug { "Connecting to LDAP..." }
    for (attempt in 1..maxAttempts) {
        try {
            return LDAPConnection(
                host,
                port,
                user,
                password
            )
        } catch (e: LDAPException) {
            if (attempt == maxAttempts) {
                throw RuntimeException("Failed to connect to $host:$port in $attempt attempts")
            }
            logger.warn(e) { "Failed to connect to $host:$port. Attempt $attempt/${maxAttempts}" }
            Thread.sleep(attempt * 1000L)
        }
    }
    error("Unreachable code")
}