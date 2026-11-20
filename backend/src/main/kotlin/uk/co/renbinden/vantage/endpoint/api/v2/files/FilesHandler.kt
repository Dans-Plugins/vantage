package uk.co.renbinden.vantage.endpoint.api.v2.files

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NO_CONTENT
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.path
import java.io.File
import java.io.FileInputStream

class FilesHandler(private val baseDirectory: File) {

    fun get(request: Request): Response {
        val path = request.path("path") ?: "."
        val file = File(baseDirectory, path)
        if (!file.isInside(baseDirectory)) {
            return Response(BAD_REQUEST)
        }
        return if (file.isDirectory) {
            Response(OK).with(
                FilesResponse.lens of FilesResponse(
                    file.listFiles()
                        ?.sortedWith { a, b ->
                            when {
                                a == b -> 0
                                a.isDirectory && !b.isDirectory -> -1
                                b.isDirectory && !a.isDirectory -> 1
                                else -> a.name.compareTo(b.name)
                            }
                        }
                        ?.map { it.toResponse() } ?: emptyList()
                )
            )
        } else {
            Response(OK).body(
                FileInputStream(file),
                file.length()
            )
        }
    }

    fun put(request: Request): Response {
        val path = request.path("path") ?: "."
        val file = File(baseDirectory, path)
        if (!file.isInside(baseDirectory)) {
            return Response(BAD_REQUEST)
        }
        val payload = request.body.payload.array()
        if (!file.parentFile.isDirectory) {
            return Response(BAD_REQUEST)
        }
        if (!file.parentFile.exists()) {
            file.parentFile.mkdirs()
        }
        file.writeBytes(payload)
        return Response(NO_CONTENT)
    }

    fun delete(request: Request): Response {
        val path = request.path("path") ?: "."
        val file = File(baseDirectory, path)
        if (!file.isInside(baseDirectory) || file.normalizedPath() == baseDirectory.normalizedPath()) {
            return Response(BAD_REQUEST)
        }
        file.deleteRecursively()
        return Response(NO_CONTENT)
    }

    // True when this file is the directory itself or anywhere below it, after resolving "." and ".."
    // segments. Symbolic links are not resolved.
    private fun File.isInside(directory: File): Boolean {
        return normalizedPath().startsWith(directory.normalizedPath())
    }

    private fun File.normalizedPath() = toPath().toAbsolutePath().normalize()

    private fun File.toResponse(): FileResponse {
        return FileResponse(
            name,
            isDirectory
        )
    }

}