import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.dao.id.IntIdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.json.json

object PersonTable : IdTable<Int>(name = "persons") {
    override val id: Column<EntityID<Int>> = integer("ID").autoIncrement().entityId()
    val name = varchar("NAME", 100)
    val age = integer("AGE")
    val details = json<PersonDetails>("DETAILS", Json.Default)
    override val primaryKey = PrimaryKey(id)
}

class PersonRepository(val database: Database) : IPersonRepository {

    override fun getPersonById(id: Int): Person? =
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

    override fun addPerson(person: Person): Int =
        transaction(database) {
            PersonTable.insertAndGetId {
                it[name] = person.name
                it[age] = person.age
                it[details] = person.details
            }.value
        }

    override fun updatePerson(id: Int, person: Person) : Unit =
        transaction(database) {
            PersonTable.update({
                PersonTable.id eq id
            }) {
                it[name] = person.name
                it[age] = person.age
                it[details] = person.details
            }
        }


    override fun deletePerson(id: Int) {
        transaction(database) {
            PersonTable.deleteWhere { PersonTable.id eq id }
        }
    }
}
