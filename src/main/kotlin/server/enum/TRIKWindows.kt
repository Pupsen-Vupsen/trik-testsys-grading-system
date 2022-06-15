package server.enum

enum class TRIKWindows(val command: String) {

    TWO_D_MODEL("TRIKStudio\\patcher.exe -f"),
    PATCHER("TRIKStudio\\2D-model.exe -b -r")
}