package com.turndapage.acsystools.models

class TaskCode(val id: Int, var name: String) {
    override fun equals(other: Any?): Boolean {
        if(other is TaskCode)
            return other.id == this.id
        return false
    }

    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return name
    }
}