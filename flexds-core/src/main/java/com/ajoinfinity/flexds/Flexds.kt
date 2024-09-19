package com.ajoinfinity.flexds

import com.ajoinfinity.flexds.exceptions.CompositeException


interface Flexds<D> : FlexdsFeatures<D> {
    fun showDataflow(): String {
        return " --> $name "
    }
}

