package server.repository

import server.entity.Submission

import org.springframework.data.repository.CrudRepository

interface SubmissionRepository: CrudRepository<Submission, String> {

    fun findSubmissionById(id: Long): Submission?

    fun findSubmissionByFilePathAndStatus(filePath: String, status: String): Submission?
}