package com.ajoinfinity.flexds.features.logger

import com.ajoinfinity.flexds.FlexDataSourceManager
import com.ajoinfinity.flexds.Logger

interface FlexdsLogger {
    val logger: Logger
        get() = FlexDataSourceManager.logger
}