import org.json.JSONObject
import java.awt.Toolkit
import java.io.BufferedReader
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream

object Main {
    const val CLIENT_JSON_LOG = "ClientJson.log"

    @JvmStatic
    fun main(args: Array<String>) {
        val localPath = getLocalPath()
        var uuid: String? = null

        val zips = localPath.listFiles { dir, name ->
            name.startsWith("ClientJson_") && name.endsWith(".gz") && File(dir, name).length() > 0
        }
        zips.sortBy {
            it.name
        }
        File(localPath, "_temp.log").also {outputFile->
            outputFile.outputStream().bufferedWriter().use { writer ->
                zips.forEach { gz ->
                    GZIPInputStream(gz.inputStream()).use {
                        val reader = it.bufferedReader()
                        reader.forEachLine { line ->
                            uuid = uuid ?: takeUuidIfNeeded(line)
                            val humanReadable = parseJson(line)
                            writer.write(humanReadable)
                            writer.newLine()
                        }
                    }
                }
                File(localPath, CLIENT_JSON_LOG).takeIf { it.exists() }?.run {
                    this.useLines {lines->
                        lines.forEach {line->
                            uuid = uuid ?: takeUuidIfNeeded(line)
                            val humanReadable = parseJson(line)
                            writer.write(humanReadable)
                            writer.newLine()
                        }
                    }
                }
            }

            if (uuid != null) {
                outputFile.renameTo(File(localPath, "$uuid.log"))
            }
        }

        Toolkit.getDefaultToolkit().beep();
    }

    private fun takeUuidIfNeeded(line: String): String? {
        JSONObject(line).getJSONArray("contextMap").forEach {
            val j = it as JSONObject
            if (j.has("key") && j.getString("key") == "uuid" && j.has("value")) {
                val rawUuid = j.getString("value")
                rawUuid.takeIf { !it.isNullOrEmpty() && it.toLowerCase() != "none" }?.also {
                    return it
                }
            }
        }
        return null
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
