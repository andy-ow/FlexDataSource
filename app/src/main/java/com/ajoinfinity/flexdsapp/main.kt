package com.ajoinfinity.flexdsapp

import com.ajoinfinity.flexds.FlexDataSourceManager
import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.FlexDSBuilder
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
    dataSource: Flexds<D>,
    users: List<D>,
) {
    // Save users
    val saveTime = measureTimeMillis {
        users.forEachIndexed { index, user ->
            dataSource.save("user$index", user)
        }
    }
    println("Time to save ${users.size} users in ${dataSource.name}: $saveTime ms")

    // Read users
    val readTime = measureTimeMillis {
        users.forEachIndexed { index, _ ->
            assert(users[index] == dataSource.findById("user$index").getOrThrow()) { "Test Failed" }
            //println("users[$index] = ${users[index]} =?= from flexds: ${dataSource.findById("user$index").getOrThrow()}")
        }
    }
    println("Time to read ${users.size} users from ${dataSource.name}: $readTime ms")
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

    // Test with MemoryDS using the updated builder
    val memoryDS = FlexDataSourceManager.memory<User>("test_memory")
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .withMaxSizeDecorator(maxSize = 10000, percentToRemove = 0.5)
        .build()

    // Test with FilesystemDS using the updated builder
    val filesystemDS = FlexDataSourceManager.filesystem(
        filesDir = filesDir,
        fdsId = "users-filesystemDS-${System.currentTimeMillis()}",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .withMaxSizeDecorator(maxSize = 20000, percentToRemove = 0.3)
        .build()

    // Test with FilesystemDS with Memory Cache using the updated builder
    val filesystemDSwithMemoryCache = FlexDataSourceManager.filesystem(
        filesDir = filesDir,
        fdsId = "users-filesystemDSwithCache-${System.currentTimeMillis()}",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withCacheDecorator(memoryDS)  // Use memory cache with filesystem
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .build()



    runBlocking {
        for (i in 1..2) performSaveAndReadTest(memoryDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..2) performSaveAndReadTest(filesystemDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..2) performSaveAndReadTest(filesystemDSwithMemoryCache, createUsers(NUMBER_OF_USERS),)

    }
}
