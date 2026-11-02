import kotlin.uuid.Uuid

interface IPersonRepository {
    fun getPersonById(id: Uuid): Person?
    fun getAllPersons(): List<Person>
    fun searchPersonsByName(query: String): List<Person>
    fun addPerson(person: Person): Uuid
    fun updatePerson(id: Uuid, person: Person)
    fun deletePerson(id: Uuid)
    fun deleteAll()
}