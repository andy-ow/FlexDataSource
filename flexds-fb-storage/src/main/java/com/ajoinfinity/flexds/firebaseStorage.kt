package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FirebaseStorageDS
import com.google.firebase.storage.FirebaseStorage
import kotlinx.serialization.KSerializer

// Convenience method to create FirebaseStorageDS
fun <D> FlexDSBuilder.Companion.firebaseStorage(
    fdsId: String,
    firebaseStorage: FirebaseStorage,
    dataClazz: Class<D>,
    serializer: KSerializer<D>?,
): FlexDSBuilder<D> {
    val metaFdsId = "${fdsId}_metadata"
    val metaFds = FirebaseStorageDS(metaFdsId, firebaseStorage, String::class.java)

    return FlexDSBuilder(
        FirebaseStorageDS(fdsId, firebaseStorage, dataClazz, ),
        metaFds = metaFds,
        dataClazz = dataClazz,
        serializer = serializer,)
}

