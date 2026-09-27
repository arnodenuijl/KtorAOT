import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
class Person (
    val id: Uuid,
    val name: String,
    val age: Int,
    val details: PersonDetails
)

@Serializable
class PersonDetails (
    val address: String,
    val phoneNumber: String
)

interface IPersonRepository {
    fun getPersonById(id: Uuid): Person?
    fun getAllPersons(): List<Person>
    fun addPerson(person: Person): Uuid
    fun updatePerson(id: Uuid, person: Person)
    fun deletePerson(id: Uuid)
    fun deleteAll()
}