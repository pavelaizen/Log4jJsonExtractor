import org.json.JSONObject
import java.awt.Toolkit
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream

object Main {
    var uuid: String? = null

    @JvmStatic
    fun main(args: Array<String>) {
        val localPath = getLocalPath()
        val zips = localPath.listFiles { dir, name ->
            name.startsWith("ClientJson_") && name.endsWith(".gz") && File(dir, name).length() > 0
        }
        zips.sortBy {
            it.name
        }
        File(localPath, "_temp.log").also {
            it.outputStream().bufferedWriter().use { writer ->
                zips.forEach { gz ->
                    GZIPInputStream(gz.inputStream()).use {
                        val reader = it.bufferedReader()
                        reader.forEachLine { line ->
                            takeUuidIfNeeded(line)
                            val humanReadable = parseJson(line)
                            writer.write(humanReadable)
                            writer.newLine()
                        }
                    }
                }
            }
            if (uuid != null) {
                it.renameTo(File(localPath, "$uuid.log"))
            }
        }

        Toolkit.getDefaultToolkit().beep();
    }

    private fun takeUuidIfNeeded(line: String) {
        if (uuid == null) {
            JSONObject(line).getJSONArray("contextMap").forEach {
                val j = it as JSONObject
                if (j.has("key") && j.getString("key") == "uuid" && j.has("value")) {
                    val rawUuid = j.getString("value")
                    rawUuid.takeIf { !it.isNullOrEmpty() && it.toLowerCase() != "none" }?.also {
                        uuid = it
                    }
                }
            }
        }
    }

    private fun parseJson(raw: String): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }
        val json = JSONObject(raw)
        val time: String = dateFormat.format(Date(json.getLong("timeMillis")))
        val lvl = json.getString("level")
        val tag = json.getString("loggerName")?.substringAfterLast(".")
        val msg = json.getString("message")
        //22/03/2018 06:45:09.356 DEBUG [EarlySense] - System Version: 7.0
        return "$time $lvl [$tag] - $msg"
    }


}
