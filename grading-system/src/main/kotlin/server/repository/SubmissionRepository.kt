package server.repository

import server.entity.Submission
import server.enum.Status

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SubmissionRepository : CrudRepository<Submission, String> {

    fun findSubmissionById(id: Long): Submission?

    fun findSubmissionByFilePathAndStatus(filePath: String, status: Status): Submission?
}