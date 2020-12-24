@file:Suppress("UNCHECKED_CAST")

package com.wifitask.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <CONTEXT, DATA_TYPE> memoized(function: () -> DATA_TYPE) =
    object : ReadWriteProperty<CONTEXT, DATA_TYPE> {
        private var initializedData = false
        private var data: DATA_TYPE? = null
        override fun getValue(thisRef: CONTEXT, property: KProperty<*>): DATA_TYPE =
            when (initializedData) {
                true -> data as DATA_TYPE
                false -> function().apply {
                    initializedData = true
                    data = this
                }

            }

        override fun setValue(thisRef: CONTEXT, property: KProperty<*>, value: DATA_TYPE) {
            data = value
        }
    }