package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FirebaseStorageDS
import com.google.firebase.storage.FirebaseStorage

// Convenience method to create FirebaseStorageDS
fun <D> FlexDataSourceManager.Companion.firebaseStorage(
    fdsId: String,
    firebaseStorage: FirebaseStorage,
    dataTypeName: String = "File"
): FlexDSBuilder<D> {
    return FlexDSBuilder(FirebaseStorageDS(fdsId, firebaseStorage, dataTypeName))
}

