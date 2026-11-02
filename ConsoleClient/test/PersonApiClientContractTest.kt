import au.com.dius.pact.consumer.MockServer
import au.com.dius.pact.consumer.dsl.PactDslJsonBody
import au.com.dius.pact.consumer.dsl.PactDslWithProvider
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt
import au.com.dius.pact.consumer.junit5.PactTestFor
import au.com.dius.pact.core.model.PactSpecVersion
import au.com.dius.pact.core.model.V4Pact
import au.com.dius.pact.core.model.annotations.Pact
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@ExtendWith(PactConsumerTestExt::class)
@PactTestFor(providerName = "WebApi", pactVersion = PactSpecVersion.V4)
class PersonApiClientContractTest {
    companion object {
        init {
            System.setProperty("pact.rootDir", File("pacts").absolutePath)
        }
    }

    private val personId = Uuid.parse("3f2504e0-4f89-41d3-9a0c-0305e82c3301")
    private val missingPersonId = Uuid.parse("00000000-0000-0000-0000-000000000001")

    @Pact(consumer = "ConsoleClient", provider = "WebApi")
    fun personExists(builder: PactDslWithProvider): V4Pact = builder
        .given("a person with id $personId exists")
        .uponReceiving("a request for an existing person")
        .path("/personApi/persons/$personId")
        .method("GET")
        .willRespondWith()
        .status(200)
        .headers(mapOf("Content-Type" to "application/json"))
        .body(
            PactDslJsonBody()
                .uuid("id", personId.toString())
                .stringType("name", "Jane Doe")
                .integerType("age", 42)
                .`object`("details")
                .stringType("address", "1 Main Street")
                .stringType("phoneNumber", "+31 6 12345678")
                .closeObject()!!
        )
        .toPact(V4Pact::class.java)

    @Pact(consumer = "ConsoleClient", provider = "WebApi")
    fun personDoesNotExist(builder: PactDslWithProvider): V4Pact = builder
        .given("no person with id $missingPersonId exists")
        .uponReceiving("a request for a missing person")
        .path("/personApi/persons/$missingPersonId")
        .method("GET")
        .willRespondWith()
        .status(404)
        .toPact(V4Pact::class.java)

    @Test
    @PactTestFor(pactMethod = "personExists")
    fun `returns the person when the provider responds with 200`(mockServer: MockServer) = runBlocking {
        val client = PersonApiClient(mockServer.getUrl())

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
    @PactTestFor(pactMethod = "personDoesNotExist")
    fun `returns null when the provider responds with 404`(mockServer: MockServer) = runBlocking {
        val client = PersonApiClient(mockServer.getUrl())

        assertNull(client.getPersonById(missingPersonId))
    }
}
