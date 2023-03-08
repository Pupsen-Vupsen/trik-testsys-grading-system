package trik.testsys.gradingsystem.enums


enum class TRIKCommands(val command: String) {

    TWO_D_MODEL("./TRIKStudio/bin/2D-model -platform offscreen --close -r"),
    PATCHER("./TRIKStudio/bin/patcher -f")
}