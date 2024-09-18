package com.ajoinfinity.poleconyksiegowy.data.datasource.baseDS

//import com.ajoinfinity.flexds.DataSource
//
//class LoggingDS(private val wrappedDataSource: DataSource, ) : DataSource {
//    override val repositoryName = "LoggingStorageRepository"
//    override val SHOULD_NOT_BE_USED_AS_CACHE: Boolean = true
//
//    override suspend fun save(id: String, data: ByteArray): Result<Unit> {
//        //println("Saving file: $fileName")
//        return wrappedDataSource.save(id, data)
//    }
//
//    override suspend fun retrieveFile(fileName: String): Result<ByteArray> {
//        //println("Retrieving file: $fileName")
//        return wrappedDataSource.retrieveFile(fileName)
//    }
//
//    override suspend fun deleteFile(id: String): Result<Unit> {
//        //println("Deleting file: $fileName")
//        return wrappedDataSource.deleteFile(id)
//    }
//
//    override suspend fun listFiles(): Result<List<String>> {
//        //println("Listing files")
//        return wrappedDataSource.listFiles()
//    }
//}
