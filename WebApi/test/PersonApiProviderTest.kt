import au.com.dius.pact.provider.junit5.HttpTestTarget
import au.com.dius.pact.provider.junit5.PactVerificationContext
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider
import au.com.dius.pact.provider.junitsupport.Provider
import au.com.dius.pact.provider.junitsupport.State
import au.com.dius.pact.provider.junitsupport.loader.PactFolder
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestTemplate
import org.junit.jupiter.api.extension.ExtendWith
import java.net.ServerSocket
import kotlin.uuid.Uuid

class InMemoryPersonRepository : IPersonRepository {
    private val persons = mutableMapOf<Uuid, Person>()

    override fun getPersonById(id: Uuid): Person? = persons[id]
    override fun getAllPersons(): List<Person> = persons.values.toList()
    override fun searchPersonsByName(query: String): List<Person> =
        persons.values.filter { it.name.contains(query, ignoreCase = true) }
    override fun addPerson(person: Person): Uuid {
        persons[person.id] = person
        return person.id
    }
    override fun updatePerson(id: Uuid, person: Person) {
        persons[id] = person
    }
    override fun deletePerson(id: Uuid) {
        persons.remove(id)
    }
    override fun deleteAll() {
        persons.clear()
    }
}

@Provider("WebApi")
@PactFolder("../ConsoleClient/pacts")
class PersonApiProviderTest {

    private val repository = InMemoryPersonRepository()
    private var server: EmbeddedServer<*, *>? = null

    @BeforeEach
    fun startProvider(context: PactVerificationContext) {
        val port = ServerSocket(0).use { it.localPort }
        server = embeddedServer(factory = CIO, port = port) {
            with(KtorApplication) { rootModule(repository) }
        }.also { it.start(wait = false) }
        context.target = HttpTestTarget("localhost", port, "/")
    }

    @AfterEach
    fun stopProvider() {
        server?.stop(gracePeriodMillis = 0, timeoutMillis = 0)
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider::class)
    fun pactVerificationTestTemplate(context: PactVerificationContext) {
        context.verifyInteraction()
    }

    @State("a person with id 3f2504e0-4f89-41d3-9a0c-0305e82c3301 exists")
    fun personExists() {
        repository.deleteAll()
        repository.addPerson(
            Person(
                id = Uuid.parse("3f2504e0-4f89-41d3-9a0c-0305e82c3301"),
                name = "Jane Doe",
                age = 42,
                details = PersonDetails(address = "1 Main Street", phoneNumber = "+31 6 12345678")
            )
        )
    }

    @State("no person with id 00000000-0000-0000-0000-000000000001 exists")
    fun personDoesNotExist() {
        repository.deleteAll()
    }
}
