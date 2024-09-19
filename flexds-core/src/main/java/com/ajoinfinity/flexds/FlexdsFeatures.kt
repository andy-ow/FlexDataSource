package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.features.FlexdsCore
import com.ajoinfinity.flexds.features.FlexdsListStoredIds
import com.ajoinfinity.flexds.features.FlexdsSize
import com.ajoinfinity.flexds.features.FlexdsGetLastModificationTime

interface FlexdsFeatures<D> :
        FlexdsCore<D>,
        FlexdsSize,
        FlexdsGetLastModificationTime,
        FlexdsListStoredIds<D>