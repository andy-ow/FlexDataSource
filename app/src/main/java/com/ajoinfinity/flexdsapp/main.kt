package com.ajoinfinity.flexdsapp

import com.ajoinfinity.flexds.FlexDataSourceManager
import com.ajoinfinity.flexds.Flexds
import com.ajoinfinity.flexds.FlexDSBuilder
import com.ajoinfinity.flexds.features.addcache.AddCacheDecorator
import com.ajoinfinity.flexds.features.addcache.BaseAddCacheDecorator
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File
import kotlin.random.Random

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
        users.forEachIndexed { index, user -> // Shuffle the list to randomize the order
            val userId = "user$index"
            dataSource.save(userId, user)
        }
    }
    println("Time to save ${users.size} users in ${dataSource.name}: $saveTime ms")

    // Read users
    val readTime = measureTimeMillis {
        repeat(10 * users.size) {
            val randomIndex = Random.nextInt(users.size) // Choose a random index
            val userId = "user$randomIndex"
            val savedUser = dataSource.findById(userId).getOrThrow()
            assert(users[randomIndex] == savedUser) { "Test Failed for $userId" }
        }
    }
    println("Time to read 10 * ${users.size} users from ${dataSource.name}: $readTime ms")
    if (dataSource is AddCacheDecorator) dataSource.displayCacheStats()
    println("Cleaning up...")
    if (dataSource is AddCacheDecorator) dataSource.clearCacheStats()
    dataSource.deleteAll()

    println("Done.")
}

fun createUsers(numberOfUsers: Int): List<User> {
    return List(numberOfUsers) {
        val randomName = getRandomString(Random.nextInt(1, 10001))  // Generate a random string of length 1 to 100
        User(name = randomName, age = it)
    }
}

fun getRandomString(length: Int): String {
    val allowedChars = ('A'..'Z') + ('a'..'z')  // Characters to use for generating the random string
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun createFS(i: Int, cache: Flexds<User>): Flexds<User> {
    return FlexDataSourceManager.indexedFilesystem(
        filesDir = File("test_storage"),
        fdsId = "B$i",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withCacheDecorator(cache)
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .build()
}

fun main() {
    println("How many users to save?")
    val NUMBER_OF_USERS = readLine()?.toInt() ?: 100
    // Create a file directory for testing
    val filesDir = File("test_storage")
    val filesDir5caches = File("test_storage_5caches")

    // Test with MemoryDS using the updated builder
    val memoryDS = FlexDataSourceManager.memory<User>("ram")
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        //.withMaxSizeDecorator(maxSize = 100_000_000, percentToRemove = 0.5, )
        .build()

    // Test with FilesystemDS using the updated builder
    val filesystemDS = FlexDataSourceManager.filesystem(
        filesDir = filesDir,
        fdsId = "fs",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        //.withMaxSizeDecorator(maxSize = 5_000_000, percentToRemove = 0.3)
        .build()

    val cache = FlexDataSourceManager.memory<User>("MemCache")
        .withSizeDecorator { user -> (5 * user.name.length + 16).toLong() }
        .withMaxSizeDecorator(maxSize = 2_000_000, percentToRemove = 0.5)
        .build()
    val cache2 = FlexDataSourceManager.memory<User>("MemCache2")
        .withSizeDecorator { user -> (5 * user.name.length + 16).toLong() }
        .withMaxSizeDecorator(maxSize = 2_000_000, percentToRemove = 0.5)
        .build()
    // Test with FilesystemDS with Memory Cache using the updated builder
    val filesystemDSwithMemoryCache = FlexDataSourceManager.filesystem(
        filesDir = filesDir,
        fdsId = "fs2",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withCacheDecorator(cache)  // Use memory cache with filesystem
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .build()

    val indexedFilesystemDS = FlexDataSourceManager.indexedFilesystem(
        filesDir = filesDir,
        fdsId = "ind",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .build()

    val indexedFilesystemDSwithCache = FlexDataSourceManager.indexedFilesystem(
        filesDir = filesDir,
        fdsId = "ind2",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withCacheDecorator(cache2)
        .withSizeDecorator { user -> (10 * user.name.length + 16).toLong() }
        .build()

    val fds5caches = FlexDataSourceManager.filesystem(
        filesDir = filesDir5caches,
        fdsId = "fs5caches",
        dataClass = User(name = "dummy", age = 100),
        serializer = User.serializer()
    )
        .withCacheDecorator(memoryDS)
        .withCacheDecorator(filesystemDS)
        .withCacheDecorator(filesystemDSwithMemoryCache)
        .withCacheDecorator(indexedFilesystemDS)
        .withCacheDecorator(indexedFilesystemDSwithCache)
        .build()
    println("dataflow: ${fds5caches.showDataflow()}")


    val B = mutableListOf(createFS(0, filesystemDS))
    for (i in 1..5) {
        B.add(createFS(i, B.last()))
    }
    val C = B.last()
    print("dataflow: ${C.showDataflow()}")
    println()


    runBlocking {
        for (i in 1..1) performSaveAndReadTest(memoryDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..1) performSaveAndReadTest(filesystemDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..1) performSaveAndReadTest(filesystemDSwithMemoryCache, createUsers(NUMBER_OF_USERS),)
        for (i in 1..1) performSaveAndReadTest(indexedFilesystemDS, createUsers(NUMBER_OF_USERS),)
        for (i in 1..1) performSaveAndReadTest(indexedFilesystemDSwithCache, createUsers(NUMBER_OF_USERS),)
        for (i in 1..1) performSaveAndReadTest(fds5caches, createUsers(NUMBER_OF_USERS),)
        for (i in 1..1) performSaveAndReadTest(C, createUsers(NUMBER_OF_USERS),)
    }


}
