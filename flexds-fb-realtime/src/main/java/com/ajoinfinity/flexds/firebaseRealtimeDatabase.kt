package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.basedatasources.FirebaseRtDatabaseDS
import com.google.firebase.database.FirebaseDatabase
import kotlinx.serialization.KSerializer

// Convenience method to create FirebaseRtDatabaseDS
fun <D> FlexDSBuilder.Companion.firebaseRealtimeDatabase(
    database: FirebaseDatabase,
    fdsId: String,
    clazz: Class<D>,  // Data class type
    serializer: KSerializer<D>? = null,  // Optional serializer for custom objects
): FlexDSBuilder<D> {
    return FlexDSBuilder(
        FirebaseRtDatabaseDS(
            database = database,
            fdsId = fdsId,
            dataClazz = clazz,
            name = "FirebaseRealtimeDb-'$fdsId'",
        ),
        dataClazz = clazz,
        serializer = serializer,
    )
}
