package server.repository

import server.entity.Submission

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface SubmissionRepository : CrudRepository<Submission, String> {

    fun findSubmissionById(id: Long): Submission?
}