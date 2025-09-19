package com.mtlc.studyplan.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mtlc.studyplan.shared.*

class Converters {
    private val gson = Gson()

    // List<String> converters
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // Map<String, Int> converters
    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String {
        return gson.toJson(value ?: emptyMap<String, Int>())
    }

    @TypeConverter
    fun toStringIntMap(value: String): Map<String, Int> {
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return gson.fromJson(value, mapType) ?: emptyMap()
    }

    // List<Int> converters
    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return gson.toJson(value ?: emptyList<Int>())
    }

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    // TaskCategory converter
    @TypeConverter
    fun fromTaskCategory(category: TaskCategory): String {
        return category.name
    }

    @TypeConverter
    fun toTaskCategory(category: String): TaskCategory {
        return try {
            TaskCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            TaskCategory.OTHER
        }
    }

    // TaskPriority converter
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String {
        return priority.name
    }

    @TypeConverter
    fun toTaskPriority(priority: String): TaskPriority {
        return try {
            TaskPriority.valueOf(priority)
        } catch (e: IllegalArgumentException) {
            TaskPriority.MEDIUM
        }
    }

    // TaskDifficulty converter
    @TypeConverter
    fun fromTaskDifficulty(difficulty: TaskDifficulty): String {
        return difficulty.name
    }

    @TypeConverter
    fun toTaskDifficulty(difficulty: String): TaskDifficulty {
        return try {
            TaskDifficulty.valueOf(difficulty)
        } catch (e: IllegalArgumentException) {
            TaskDifficulty.MEDIUM
        }
    }

    // AchievementCategory converter
    @TypeConverter
    fun fromAchievementCategory(category: AchievementCategory): String {
        return category.name
    }

    @TypeConverter
    fun toAchievementCategory(category: String): AchievementCategory {
        return try {
            AchievementCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            AchievementCategory.GENERAL
        }
    }
}