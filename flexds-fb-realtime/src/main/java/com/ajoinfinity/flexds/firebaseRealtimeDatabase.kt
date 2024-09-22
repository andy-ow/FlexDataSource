package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FirebaseRtDatabaseDS
import com.ajoinfinity.flexds.main.FlexDSBuilder
import com.google.firebase.database.FirebaseDatabase
import kotlinx.serialization.KSerializer

// Convenience method to create FirebaseRtDatabaseDS
fun <D> FlexDSBuilder.Companion.firebaseRealtimeDatabase(
    database: FirebaseDatabase,
    fdsId: String,
    clazz: Class<D>,  // Data class type
    serializer: KSerializer<D>? = null,  // Optional serializer for custom objects
    dataTypeName: String = "Data"
): FlexDSBuilder<D> {
    return FlexDSBuilder(
        FirebaseRtDatabaseDS(
            database,
            fdsId,
            clazz,
            serializer,
            name = "FirebaseRealtimeDb-'$fdsId'",
            dataTypeName = dataTypeName
        )
    )
}
