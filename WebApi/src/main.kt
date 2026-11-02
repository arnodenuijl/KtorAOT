@file:OptIn(ExperimentalKtorApi::class)

import KtorApplication.rootModule
import kotlin.uuid.Uuid
import io.ktor.http.*
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.plugins.swagger.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.*
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.util.toMap
import io.ktor.utils.io.*
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.slf4j.LoggerFactory

object ApplicationKt {
    private val logger = LoggerFactory.getLogger(ApplicationKt::class.java)
    @JvmStatic
    fun main(args: Array<String>) {
        logger.info("Application Kt")
        val host = System.getenv("ORACLE_HOST") ?: "localhost"
        val database = Database.connect(
            url = "jdbc:oracle:thin:@//$host:1521/FREEPDB1?oracle.jdbc.jsonDefaultGetObjectType=java.lang.String",
            driver = "oracle.jdbc.OracleDriver",
            user = "ktor",
            password = "ktor"
        )

        val repos = PersonRepository(database).apply {
            // Example usage of the repository
            val newPersonId = addPerson(
                Person(
                    Uuid.fromLongs(0, 1), "John Doe", 30, PersonDetails(
                        "Some details",
                        phoneNumber = "06-12345678"
                    )
                )
            )
            println("Added person with ID: $newPersonId")
            // Example usage of the repository
            val newPersonId2 = addPerson(
                Person(
                    Uuid.fromLongs(0, 2), "Mary Doe", 30, PersonDetails(
                        "Some details",
                        phoneNumber = "06-12345678"
                    )
                )
            )
            println("Added person with ID: $newPersonId2")
            val person = getPersonById(newPersonId)
            println("Retrieved person: $person")
        }

        embeddedServer(
            factory = CIO,
            port = 9000
        ) {

            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            install(XForwardedHeaders) {
                // Optionally specify trusted proxies if needed
            }
            rootModule(repos)
        }.start(wait = true)
    }
}

object KtorApplication {
    fun Application.rootModule(repos: PersonRepository) {
        configureRouting(repos)
    }
}


fun Application.configureRouting(repos: PersonRepository) {
    routing {

        route("personApi") {

            swaggerUI(path = "swagger") {
                info = OpenApiInfo("My API", "1.0")
                source = OpenApiDocSource.Routing(ContentType.Application.Json)
            }

            get("/") {
                val request = call.request
                call.respond(
                    RequestEcho(
                        url = request.origin.let { "${it.scheme}://${it.serverHost}:${it.serverPort}${it.uri}" },
                        headers = request.headers.toMap(),
                        body = call.receiveText()
                    )
                )
            }

            get("/listDir") {
                val directoryListing = try {
                    getDirectoryListing()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error listing directory: ${e.message}")
                    return@get
                }
                call.respond(directoryListing)
            }
            get("/greet/{name}") {
                val name = call.parameters["name"] ?: "World"
                when (name) {
                    "arno" -> call.respond(HttpStatusCode.OK, Greeting("Hello Arno!"))
                    "" -> call.respond(HttpStatusCode.BadRequest, "Name parameter is empty")
                    else -> call.respond(HttpStatusCode.Created, Greeting2("Hello $name!"))
                }
            }

            personRoutes(repos)

        }
    }
}

private fun Route.personRoutes(repos: PersonRepository) {
    route("persons") {
        get("/{id}") {
            val id = call.parameters["id"]?.let { Uuid.parse(it) }
            if (id == null) {
                call.respond<String>(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            val person = repos.getPersonById(id)
            if (person == null) {
                call.respond<String>(HttpStatusCode.NotFound, "Person not found")
            } else {
                call.respond<Person>(person)
            }
        }.describe { summary = "Get a person by ID" }

        get {
            val persons = repos.getAllPersons()
            call.respond<List<Person>>(persons)
        }.describe { summary = "Get all persons" }

        this.post {
            val person = call.receive<Person>()
            val newId = repos.addPerson(person)
            call.respond<Map<String, Uuid>>(HttpStatusCode.Created, mapOf("id" to newId))
        }.describe { summary = "Add a new person" }

        this.put("/{id}") {
            val id = call.parameters["id"]?.let { Uuid.parse(it) }
            if (id == null) {
                call.respond<String>(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }
            val person = call.receive<Person>()
            repos.updatePerson(id, person)
            call.respond<HttpStatusCode>(HttpStatusCode.OK)
        }.describe { summary = "Update a person by ID" }

        delete("/{id}") {
            val id = call.parameters["id"]?.let { Uuid.parse(it) }
            if (id == null) {
                call.respond<String>(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            repos.deletePerson(id)
            call.respond<HttpStatusCode>(HttpStatusCode.OK)
        }.describe { summary = "Delete a person by ID" }

        delete {
            repos.deleteAll()
            call.respond<HttpStatusCode>(HttpStatusCode.OK)
        }.describe { summary = "Delete all persons" }

        get("/search") {
            val name = call.request.queryParameters["name"]
            if (name.isNullOrBlank()) {
                call.respond<String>(HttpStatusCode.BadRequest, "Query parameter 'name' is required")
                return@get
            }
            call.respond<List<Person>>(repos.searchPersonsByName(name))
        }.describe { summary = "Search persons by name (case insensitive, contains)" }
    }.describe {
        tag("Persons")
    }
}
