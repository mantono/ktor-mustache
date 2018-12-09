# ktor-mustahce
Use [Mustahce](https://github.com/spullara/mustache.java) effortless in [Ktor](http://ktor.io/)

### Example
```kotlin
routing {
    // Install feature with optional config block
    // Values shown below are default values
    install(Mustache) {
        // Set location for where templates are stored
        this.resources = File("src/main/resources/templates")
        this.bufferSize = 64
    }
    get("/") {
        // Use file test.mustache ('.mustache' can be omitted)
        // with substitution 'bar' for variable 'foo'
        call.respondTemplate("test") {
            mapOf("foo" to "bar")
        }
    }
}
```
