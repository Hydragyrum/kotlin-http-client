import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.exists

fun main(args: Array<String>) {
    val parser = ArgParser("kotlin-http-client")
    val url by parser.argument(ArgType.String, fullName = "URL", description = "The URL to get")
    val method by parser.option(
        ArgType.String,
        fullName = "method",
        shortName = "X",
        description = "HTTP Method to use"
    ).default("GET")
    val headers by parser.option(
        ArgType.String,
        fullName = "header",
        shortName = "H",
        description = "Header to add to the request (Name:Value)"
    ).multiple()
    val verbose by parser.option(
        ArgType.Boolean,
        fullName = "verbose",
        shortName = "v",
        description = "Print verbose information"
    ).default(false)
    val body by parser.option(ArgType.String, fullName = "body", shortName = "B", description = "Request Body")
    val output by parser.option(ArgType.String, fullName = "output", shortName = "o", description = "Output file")
    val setExec by parser.option(
        ArgType.Boolean,
        fullName = "setExec",
        shortName = "x",
        description = "Set the downloaded file as executable"
    ).default(false)

    parser.parse(args)
    val client = HttpClient(CIO)

    runBlocking {
        val response: HttpResponse = client.request(url) {
            this.method = HttpMethod.parse(method)
            this.headers {
                headers.map {
                    it.substringBefore(":") to it.substringAfter(":")
                }.forEach {
                    append(it.first, it.second)
                }
            }
            body?.let { this.body = it }

            if (verbose) {
                onDownload { bytesSentTotal, contentLength ->
                    println("Received $bytesSentTotal bytes from $contentLength")
                }
            }
        }

        println(response.status)
        if (verbose) {
            println(response.headers.flattenEntries().joinToString("\n") { "${it.first}: ${it.second}" })
            println("-".repeat(20))
        }
        val byteArrayBody: ByteArray = response.receive()
        if (verbose || output.isNullOrBlank()) {
            println(byteArrayBody.decodeToString())
        }
        if (!output.isNullOrBlank()) {
            val file = File(output)
            if (!file.exists()) {
                if (file.parentFile?.toPath()?.exists() != true) {
                    file.parentFile?.mkdirs()
                }
                file.createNewFile()
            }
            file.writeBytes(byteArrayBody)
            file.setExecutable(setExec)
            println("Saved to $output")
        }
    }

}