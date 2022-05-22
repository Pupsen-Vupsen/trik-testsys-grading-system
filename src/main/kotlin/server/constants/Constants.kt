package server.constants

class Constants {

    object Paths {
        const val TASKS = "/tasks/"
        const val TESTS = "/tests"
    }

    object Requests {
        const val SUBMISSION_NOT_FOUND = "Submission with this id does not exist."
        const val NO_SUBMISSIONS = "There is no submissions."
        const val EMPTY_FILE = "Uploading file is empty."
    }

    object TRIKWindows {
        const val TWO_D_MODEL = "TRIKStudio\\patcher.exe -f"
        const val PATCHER = "TRIKStudio\\2D-model.exe -b -r"
    }

    object TRIKLinux {
        const val TWO_D_MODEL = "./TRIKStudio/bin/2D-model -platform offscreen -s 10 -b -r"
        const val PATCHER = "./TRIKStudio/bin/patcher -f"
    }

    object Status {
        const val RUNNING = "?"
        const val OK = "+"
        const val FAILED = "-"
    }

    object ID {
        const val FIRST_ID = 1_000_000L
        const val DEFAULT_ID = -1L
    }

    object Time {
        const val WAIT_TIME = 10_000L
    }
}