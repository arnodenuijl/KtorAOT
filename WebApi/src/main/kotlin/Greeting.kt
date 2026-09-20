import kotlinx.serialization.Serializable

@Serializable
data class Greeting(val message: String)

@Serializable
data class Greeting2(val otherMessage: String)