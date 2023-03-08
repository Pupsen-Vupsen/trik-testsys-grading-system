package trik.testsys.gradingsystem.controllers

import org.springframework.web.multipart.MultipartFile

import trik.testsys.gradingsystem.enums.FilePostfixes
import trik.testsys.gradingsystem.enums.Paths

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream


class FileUploader(private val file: MultipartFile, private val submissionId: Long) {

    fun upload(): Boolean {
        if (file.isEmpty) return false

        val bytes = file.bytes
        if(!bytes.isQrsFormat()) return false

        val uploadingFilePath = Paths.SUBMISSIONS.text + "/$submissionId"
        File(uploadingFilePath).mkdir()

        val uploadedFile = File(uploadingFilePath + "/submission" + FilePostfixes.QRS.text)
        val stream = BufferedOutputStream(FileOutputStream(uploadedFile))

        stream.write(bytes)
        stream.close()

        return true
    }

    private fun ByteArray.isQrsFormat(): Boolean {
        val qrsFileHeader = intArrayOf(80, 75, 3, 4, 20, 0, 8, 0, 0, 0)

        if(size < qrsFileHeader.size) return false

        for(i in qrsFileHeader.indices) {
            if(this[i].toInt() != qrsFileHeader[i]) return false
        }

        return true
    }
}