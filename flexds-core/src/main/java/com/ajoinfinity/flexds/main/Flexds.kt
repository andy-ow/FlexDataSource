package com.ajoinfinity.flexds.main

import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsAddCache
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsAddMetadata
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsCoreFeatures
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsListStoredIds
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsSize
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsGetDbLastModificationTime
import com.ajoinfinity.flexds.main.featureinterfaces.FlexdsMaxSize

interface Flexds<D> :
        FlexdsAddCache<D>,
        FlexdsAddMetadata,
        FlexdsCoreFeatures<D>,
        FlexdsGetDbLastModificationTime,
        FlexdsListStoredIds<D>,
        FlexdsMaxSize,
        FlexdsSize<D>
