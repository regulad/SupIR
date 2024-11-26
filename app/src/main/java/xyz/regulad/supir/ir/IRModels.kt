package xyz.regulad.supir.ir

import kotlinx.serialization.Serializable

@Serializable
data class SBrand(val name: String, val categories: List<SCategory>) {
    fun merge(other: SBrand): SBrand {
        if (name != other.name) {
            throw IllegalArgumentException("Cannot merge brands with different names")
        }

        val newCategories = categories.toMutableList()

        for (category in other.categories) {
            val existingCategory = newCategories.find { it.name == category.name }
            if (existingCategory != null) {
                newCategories.remove(existingCategory)
                newCategories.add(existingCategory.merge(category))
            } else {
                newCategories.add(category)
            }
        }

        return SBrand(name, newCategories.sortedBy { it.name })
    }
}

@Serializable
data class SCategory(val name: String, val models: List<SModel>) {
    fun merge(other: SCategory): SCategory {
        if (name != other.name) {
            throw IllegalArgumentException("Cannot merge categories with different names")
        }

        val newModels = models.toMutableList()

        for (model in other.models) {
            val existingModel = newModels.find { it.identifier == model.identifier }
            if (existingModel != null) {
                newModels.remove(existingModel)
                newModels.add(existingModel.merge(model))
            } else {
                newModels.add(model)
            }
        }

        return SCategory(name, newModels.sortedBy { it.identifier })
    }
}

@Serializable
data class SModel(val identifier: String, val functions: List<IRFunction>) {
    fun merge(other: SModel): SModel {
        if (identifier != other.identifier) {
            throw IllegalArgumentException("Cannot merge models with different identifiers")
        }

        val newFunctions = functions.toMutableList()
        newFunctions.addAll(other.functions)

        return SModel(identifier, newFunctions.sortedBy { it.identifier })
    }
}
