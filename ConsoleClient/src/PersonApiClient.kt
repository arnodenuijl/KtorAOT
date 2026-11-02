import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.uuid.Uuid

class PersonApiClient(
    baseUrl: String,
    private val httpClient: HttpClient = HttpClient(CIO),
) : AutoCloseable {
    private val baseUrl = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getPersonById(id: Uuid): Person? {
        val response = httpClient.get("$baseUrl/personApi/persons/$id")
        return when (response.status) {
            HttpStatusCode.OK -> json.decodeFromString<PersonDto>(response.bodyAsText()).toPerson()
            HttpStatusCode.NotFound -> null
            else -> throw IllegalStateException(
                "Get person by id failed with status ${response.status}: ${response.bodyAsText()}"
            )
        }
    }

    override fun close() {
        httpClient.close()
    }
}

@Serializable
private data class PersonDto(
    val id: String,
    val name: String,
    val age: Int,
    val details: PersonDetailsDto,
)

@Serializable
private data class PersonDetailsDto(
    val address: String,
    val phoneNumber: String,
)

private fun PersonDto.toPerson() = Person(
    id = Uuid.parse(id),
    name = name,
    age = age,
    address = details.address,
    phoneNumber = details.phoneNumber,
)
