package trik.testsys.gradingsystem.repositories

import org.springframework.data.relational.core.mapping.Table
import trik.testsys.gradingsystem.entities.Submission

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository


@Repository
@Table(name = "SUBMISSIONS")
interface SubmissionRepository : CrudRepository<Submission, String> {

    fun findSubmissionById(id: Long): Submission?
}