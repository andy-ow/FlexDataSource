package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.features.FlexdsAddCache
import com.ajoinfinity.flexds.features.FlexdsAddMetadata
import com.ajoinfinity.flexds.features.FlexdsCoreFeatures
import com.ajoinfinity.flexds.features.FlexdsListStoredIds
import com.ajoinfinity.flexds.features.FlexdsSize
import com.ajoinfinity.flexds.features.FlexdsGetDbLastModificationTime
import com.ajoinfinity.flexds.features.FlexdsMaxSize

interface Flexds<D> :
        FlexdsCoreFeatures<D>,
        FlexdsSize<D>,
        FlexdsMaxSize,
        FlexdsGetDbLastModificationTime,
        FlexdsListStoredIds<D>,
        FlexdsAddCache<D>,
        FlexdsAddMetadata