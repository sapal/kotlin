// "Remove variable 'b'" "true"
class Foo {
    var foo = 5
    fun changeFoo() : Int {
        foo = 10
        return foo
    }
}
fun f() : Foo {
    var a = Foo()
    <caret>a.changeFoo()
    return a
}
