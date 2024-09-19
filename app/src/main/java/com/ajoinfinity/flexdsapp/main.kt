package com.ajoinfinity.flexdsapp

import com.ajoinfinity.flexds.DataSource
import com.ajoinfinity.flexds.ObjectSizeFetcher
import com.ajoinfinity.flexds.basedatasources.FilesystemDS
import com.ajoinfinity.flexds.basedatasources.MemoryDS
import com.ajoinfinity.flexds.combinedDS.FilesystemDSwithMemoryCacheFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File

import kotlin.system.measureTimeMillis

// A simple data class that will be serialized
@Serializable
data class User(val name: String, val age: Int)

// Helper function to save and read users in the given DataSource
suspend fun <D> performSaveAndReadTest(
    dataSource: DataSource<D>,
    users: List<D>,
) {
    // Save users
    val calculatingSize = measureTimeMillis {
        println("GetByteSize(users) = ${ObjectSizeFetcher.getObjectSize(users)}")
    }
    println("Time to get byte size of ${users.size} users: $calculatingSize ms")

    // Save users
    val saveTime = measureTimeMillis {
        users.forEachIndexed { index, user ->
            dataSource.save("user$index", user)
        }
    }
    println("Time to save ${users.size} users in ${dataSource.dsName}: $saveTime ms")

    // Read users
    val readTime = measureTimeMillis {
        users.forEachIndexed { index, _ ->
            assert(users[index] == dataSource.findById("user$index").getOrThrow()) { "Test Failed" }
        }
    }
    println("Time to read ${users.size} users from ${dataSource.dsName}: $readTime ms")
    print("Cleaning up. ")
    val cleanUp = dataSource.deleteAll()
    if (cleanUp.isFailure) println("Error while cleaning up: ${cleanUp.exceptionOrNull()}")
    else println("Done.")
}

fun createUsers(numberOfUsers: Int): List<User> {
    return List(numberOfUsers) {
        User(name = "User$it-${System.currentTimeMillis()}", age = it)
    }
}

fun main() {
    println("How many users to save?")
    val NUMBER_OF_USERS = readLine()?.toInt() ?: 100
    // Create a file directory for testing
    val filesDir = File("test_storage")


    // Test with MemoryDS
    val memoryDS = MemoryDS<User>("test_memory")

    // Test with FilesystemDS
    val filesystemDS = FilesystemDS(
        dataSourceId = "users-filesystemDS-${System.currentTimeMillis()}",
        dataClass = User(name = "dummy", age = 100),
        filesDir = filesDir,
        serializer = User.serializer()
    )


    val filesystemDSwithMemoryCache = FilesystemDSwithMemoryCacheFactory<User>()
        .create(
            filesDir = filesDir,
            dataSourceId = "users-filesystemDSwithCache-${System.currentTimeMillis()}",
            getSize = { user -> (10 * user.name.length + 16).toLong() },
            serializer = User.serializer()
        )
    runBlocking {
        for (i in 1..2) performSaveAndReadTest(memoryDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..2) performSaveAndReadTest(filesystemDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..2) performSaveAndReadTest(filesystemDSwithMemoryCache, createUsers(NUMBER_OF_USERS),)

    }
}
