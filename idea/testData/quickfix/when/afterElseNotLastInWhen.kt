// "Move else branch to the end" "true"
fun test() {
    val a = 12
    when (a) {
        1 -> { /* some code */ }
        2 -> { /* some more code */ }
        el<caret>se -> { /* other code */ }
    }
}