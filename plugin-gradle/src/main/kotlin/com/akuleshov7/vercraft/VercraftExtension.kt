package com.akuleshov7.vercraft

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

class VercraftExtension {
    lateinit var type: Property<String>

    constructor(objectFactory: ObjectFactory) {
        this.type = objectFactory.property(String::class.java)
    }
}