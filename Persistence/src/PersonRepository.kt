import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json
import kotlin.uuid.Uuid

object PersonTable : IdTable<Uuid>(name = "persons") {
    override val id: Column<EntityID<Uuid>> = uuid("ID").clientDefault { Uuid.random() }.entityId()
    val name = varchar("NAME", 100)
    val age = integer("AGE")
    val details = json<PersonDetails>("DETAILS", Json.Default)
    override val primaryKey = PrimaryKey(id)
}

class PersonRepository(val database: Database) : IPersonRepository {

    override fun getPersonById(id: Uuid): Person? =
        transaction(database) {
            val row =
                PersonTable
                    .selectAll()
                    .where { PersonTable.id eq id }
                    .singleOrNull()
            row?.let {
                Person(
                    id = it[PersonTable.id].value,
                    name = it[PersonTable.name],
                    age = it[PersonTable.age],
                    details = it[PersonTable.details]
                )
            }
        }

    override fun getAllPersons(): List<Person> =
        transaction(database) {
            PersonTable
                .selectAll()
                .map {
                    Person(
                        id = it[PersonTable.id].value,
                        name = it[PersonTable.name],
                        age = it[PersonTable.age],
                        details = it[PersonTable.details]
                    )
                }
        }

    override fun searchPersonsByName(query: String): List<Person> =
        transaction(database) {
            PersonTable
                .selectAll()
                .where { PersonTable.name.lowerCase() like "%${query.lowercase()}%" }
                .map {
                    Person(
                        id = it[PersonTable.id].value,
                        name = it[PersonTable.name],
                        age = it[PersonTable.age],
                        details = it[PersonTable.details]
                    )
                }
        }

    override fun addPerson(person: Person): Uuid =
        transaction(database) {
            PersonTable.insertAndGetId {
                it[name] = person.name
                it[age] = person.age
                it[details] = person.details
            }.value
        }

    override fun updatePerson(id: Uuid, person: Person) : Unit =
        transaction(database) {
            PersonTable.update({
                PersonTable.id eq id
            }) {
                it[name] = person.name
                it[age] = person.age
                it[details] = person.details
            }
        }


    override fun deletePerson(id: Uuid) {
        transaction(database) {
            PersonTable.deleteWhere { PersonTable.id eq id }
        }
    }

    override fun deleteAll() {
        transaction(database) {
            PersonTable.deleteAll()
        }
    }
}
