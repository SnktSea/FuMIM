package snkt.org

import snkt.org.model.UserShort
import kotlin.reflect.KClass

fun <T : Any> compareADObjectsToLocal(
    domain: String,
    adObjects: List<T>,
    localObjects: List<T>
): Pair<List<T>, List<T>> {
    val localUserSet = localObjects.toSet()
    val adUserSet = adObjects.toSet()

    val cleanList = (localUserSet - adUserSet).toList()
    logger.debug { "Amount of objects to delete from local storage ${cleanList.size}" }
    val pullList = (adUserSet - localUserSet).toList()
    logger.debug { adUserSet.toString() }
    logger.debug { "Amount of objects to pull from AD $domain: ${pullList.size}" }

    logger.debug { "Pull list: $pullList" }
    logger.debug { "Clean list: $cleanList" }
    return cleanList to pullList
}