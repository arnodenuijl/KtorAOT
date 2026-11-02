import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class PersonApiClientTest {
    private val personId = Uuid.parse("3f2504e0-4f89-41d3-9a0c-0305e82c3301")

    private val personJson = """
        {
          "id": "3f2504e0-4f89-41d3-9a0c-0305e82c3301",
          "name": "Jane Doe",
          "age": 42,
          "details": {
            "address": "1 Main Street",
            "phoneNumber": "+31 6 12345678"
          }
        }
    """.trimIndent()

    private fun MockRequestHandleScope.jsonResponse(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(
        content = body,
        status = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )

    private fun clientFor(engine: MockEngine, baseUrl: String = "https://example.com") =
        PersonApiClient(baseUrl, HttpClient(engine))

    @Test
    fun `maps a 200 response to Person`() = runBlocking {
        val client = clientFor(MockEngine { jsonResponse(personJson) })

        val person = client.getPersonById(personId)

        assertEquals(
            Person(
                id = personId,
                name = "Jane Doe",
                age = 42,
                address = "1 Main Street",
                phoneNumber = "+31 6 12345678",
            ),
            person,
        )
    }

    @Test
    fun `calls the get person endpoint with the id`() = runBlocking {
        var requestedUrl: String? = null
        val client = clientFor(MockEngine { request ->
            requestedUrl = request.url.toString()
            jsonResponse(personJson)
        })

        client.getPersonById(personId)

        assertEquals("https://example.com/personApi/persons/$personId", requestedUrl)
    }

    @Test
    fun `trims trailing slash from base url`() = runBlocking {
        var requestedUrl: String? = null
        val client = clientFor(MockEngine { request ->
            requestedUrl = request.url.toString()
            jsonResponse(personJson)
        }, baseUrl = "https://example.com/")

        client.getPersonById(personId)

        assertEquals("https://example.com/personApi/persons/$personId", requestedUrl)
    }

    @Test
    fun `ignores unknown fields in the response`() = runBlocking {
        val body = personJson.replace(
            "\"age\": 42,",
            "\"age\": 42, \"extra\": \"ignored\",",
        )
        val client = clientFor(MockEngine { jsonResponse(body) })

        assertEquals(42, client.getPersonById(personId)?.age)
    }

    @Test
    fun `returns null on 404`() = runBlocking {
        val client = clientFor(MockEngine { jsonResponse("", HttpStatusCode.NotFound) })

        assertNull(client.getPersonById(personId))
    }

    @Test
    fun `throws on server error`() = runBlocking {
        val client = clientFor(MockEngine { jsonResponse("boom", HttpStatusCode.InternalServerError) })

        val error = assertFailsWith<IllegalStateException> {
            client.getPersonById(personId)
        }
        assertContains(error.message.orEmpty(), "500")
    }

    @Test
    fun `throws on malformed body`() = runBlocking<Unit> {
        val client = clientFor(MockEngine { jsonResponse("{ not json") })

        assertFailsWith<Exception> {
            client.getPersonById(personId)
        }
    }
}
