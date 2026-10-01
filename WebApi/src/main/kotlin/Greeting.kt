import kotlinx.serialization.Serializable

@Serializable
data class Greeting(val message: String)

@Serializable
data class Greeting2(val otherMessage: String)

@Serializable
data class RequestEcho(
    val url: String,
    val headers: Map<String, List<String>>,
    val body: String
)
