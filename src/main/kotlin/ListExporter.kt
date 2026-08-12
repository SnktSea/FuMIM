package snkt.org

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.File
import kotlin.reflect.KClass

fun <T : Any> exportListToCsv(data: List<T>, clazz: KClass<T>, filePath: String) {
    logger.info { "Exporting to $filePath" }
    File(filePath).bufferedWriter().use { writer ->
        val classPackage = clazz.java.packageName ?: ""
        val isPrimitiveOrString = clazz.java.isPrimitive ||
                classPackage.startsWith("java.") ||
                classPackage.startsWith("kotlin.")

        if (isPrimitiveOrString) {
            val csvFormat = CSVFormat.DEFAULT.builder().setHeader("Value").get()
            CSVPrinter(writer, csvFormat).use { csvPrinter ->
                for (item in data) {
                    csvPrinter.printRecord(item)
                }
            }
        } else {
            val fields = clazz.java.declaredFields
            val usableFields = fields.filter { !it.isSynthetic }

            val headers = usableFields.map { it.name }.toTypedArray()
            val csvFormat = CSVFormat.DEFAULT.builder().setHeader(*headers).get()

            CSVPrinter(writer, csvFormat).use { csvPrinter ->
                for (item in data) {
                    val rowValues = usableFields.map { field ->
                        try {
                            field.isAccessible = true
                            field.get(item) ?: ""
                        } catch (e: Exception) {
                            logger.error(e) { "Error while importing field $item" }
                        }
                    }
                    csvPrinter.printRecord(rowValues)
                }
            }
        }
    }
}
