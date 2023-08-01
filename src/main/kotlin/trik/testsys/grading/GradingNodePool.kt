package trik.testsys.grading

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import trik.testsys.gradingsystem.enums.Status
import java.io.File
import java.net.UnknownHostException

internal class GradingNodePool(
    private val graderConfigPath: String,
    private val onFinished: (submissionId: Long, status: Status) -> Unit

) {

    private val client = HttpClient(CIO)

    private val nodesPool = mutableListOf<GradingNode>()

    init {
        createNodes()
    }

    //region GradingNode creating
    private fun createNodes(){
        val configFile = File(graderConfigPath)
        if (!configFile.exists())
            throw Grader.IncorrectConfigException("Config file doesn't exist")

        val lines = configFile.readLines()
        if (lines.size < 2)
            throw Grader.IncorrectConfigException("Config size less then 2 lines")

        val trikStudioImage = lines[0]

        for (i in 1 until lines.size) {
            val nodeConfig = lines[i].split(" ")
            if (nodeConfig.size != 2)
                throw Grader.IncorrectConfigException("Incorrect node config line $i: ${lines[i]}")
            try {
                val processesCount = nodeConfig[0].toInt()
                val ip = nodeConfig[1]
                //InetAddress.getByName(ip)
                nodesPool.add(GradingNode(
                    processesCount,
                    120,
                    client,
                    ip,
                    trikStudioImage,
                    onFinished
                ))
            } catch (e: NumberFormatException) {
                throw Grader.IncorrectConfigException("Incorrect processes count at line $i: ${lines[i]}")
            } catch (e: UnknownHostException) {
                throw Grader.IncorrectConfigException("Incorrect ip at line $i: ${lines[i]}")
            }
        }
    }
    //endregion

    private fun chooseNode(): GradingNode {
        return nodesPool.maxByOrNull { x -> x.availableProcesses() }!!
    }

    fun poll() {
//        for (node in nodesPool)
//            node.poll()
    }

    fun grade(submissionInfo: GradingNode.SubmissionInfo) {
        val node = chooseNode()
        node.gradeSubmission(submissionInfo)
    }

}