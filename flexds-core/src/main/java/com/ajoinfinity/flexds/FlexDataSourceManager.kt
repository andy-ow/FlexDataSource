package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.features.logger.DefaultLogger
import com.ajoinfinity.flexds.features.logger.Logger


class FlexDataSourceManager {
    companion object {
        private var _logger: Logger = DefaultLogger()

        var logger: Logger
            get() = _logger
            set(value) {
                _logger = value
            }
    }
}
