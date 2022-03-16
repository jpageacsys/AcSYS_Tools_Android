package com.turndapage.acsystools.models

class Project(public var id: Int, var code: Int, var name: String) {
    override fun equals(other: Any?): Boolean {
        if(other is Project)
            return other.id == this.id
        return false
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return if(code > -1)
            "$code - $name"
        else
            name
    }
}