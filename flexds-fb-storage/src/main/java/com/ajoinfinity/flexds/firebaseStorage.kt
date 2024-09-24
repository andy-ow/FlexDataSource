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
    dataTypeName: String = "File"
): FlexDSBuilder<D> {
    return FlexDSBuilder(
        FirebaseStorageDS(fdsId, firebaseStorage, dataClazz, dataTypeName),
        dataClazz = dataClazz,
        serializer = serializer,)
}

