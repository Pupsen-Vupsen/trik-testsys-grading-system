package server.service

import org.springframework.web.multipart.MultipartFile

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class FileUploader(private val file: MultipartFile, private val fileName: String) {

    fun upload(directory: String): Boolean {
        if (file.isEmpty) return false

        val bytes = file.bytes
        val stream = BufferedOutputStream(FileOutputStream(File(fileName)))
        stream.write(bytes)
        stream.close()

        return true
    }
}