import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.request.receive
import io.ktor.server.routing.openapi.describe
import io.ktor.utils.io.ExperimentalKtorApi

@OptIn(ExperimentalKtorApi::class)
fun Routing.personRoutes(repos: IPersonRepository) {

    route("/persons") {
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@get
            }
            val person = repos.getPersonById(id)
            if (person == null) {
                call.respond(HttpStatusCode.NotFound, "Person not found")
            } else {
                call.respond(person)
            }
        }.describe { summary = "Get a person by ID" }

        get {
            val persons = repos.getAllPersons()
            call.respond(persons)
        }.describe { summary = "Get all persons" }

        post {
            val person = call.receive<Person>()
            val newId = repos.addPerson(person)
            call.respond(HttpStatusCode.Created, mapOf("id" to newId))
        }.describe { summary = "Add a new person" }

        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@put
            }
            val person = call.receive<Person>()
            repos.updatePerson(id, person)
            call.respond(HttpStatusCode.OK)
        }.describe { summary = "Update a person by ID" }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid ID")
                return@delete
            }
            repos.deletePerson(id)
            call.respond(HttpStatusCode.OK)
        }.describe { summary = "Delete a person by ID" }

        delete {
            repos.deleteAll()
            call.respond(HttpStatusCode.OK)
        }.describe { summary = "Delete all persons" }
    }.describe {
        tag("Persons")
    }
}
