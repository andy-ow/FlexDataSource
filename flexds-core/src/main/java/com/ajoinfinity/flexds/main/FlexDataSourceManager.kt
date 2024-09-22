package com.ajoinfinity.flexds.main

import com.ajoinfinity.flexds.main.logger.DefaultLogger
import com.ajoinfinity.flexds.main.logger.Logger


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
