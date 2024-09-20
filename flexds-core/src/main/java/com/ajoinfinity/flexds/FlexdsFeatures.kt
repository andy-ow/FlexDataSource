package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.features.FlexdsAddCache
import com.ajoinfinity.flexds.features.FlexdsCore
import com.ajoinfinity.flexds.features.FlexdsDelete
import com.ajoinfinity.flexds.features.FlexdsListStoredIds
import com.ajoinfinity.flexds.features.FlexdsSize
import com.ajoinfinity.flexds.features.FlexdsGetDbLastModificationTime
import com.ajoinfinity.flexds.features.logger.FlexdsLogger

interface FlexdsFeatures<D> :
        FlexdsLogger,
        FlexdsCore<D>,
        FlexdsDelete<D>,
        FlexdsSize<D>,
        FlexdsGetDbLastModificationTime,
        FlexdsListStoredIds<D>,
        FlexdsAddCache<D>