import java.io.File

fun getLocalPath(): File {
    var absolutePath = Main::class.java.protectionDomain.codeSource.location.path
    absolutePath = absolutePath.substring(0, absolutePath.lastIndexOf("/"))
    absolutePath = absolutePath.replace("%20".toRegex(), " ") // Surely need to do this here
    return File(absolutePath)
}