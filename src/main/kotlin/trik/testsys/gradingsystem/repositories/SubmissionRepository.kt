package trik.testsys.gradingsystem.repositories

import trik.testsys.gradingsystem.entities.Submission

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
interface SubmissionRepository : CrudRepository<Submission, String> {

    fun findSubmissionById(id: Long): Submission?
}