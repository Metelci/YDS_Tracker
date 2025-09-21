package com.mtlc.studyplan.error

sealed class AppError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class ValidationError(override val message: String, val errors: Map<String, String>) : AppError(message)
    data class UnknownError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class DatabaseError(val msg: String, val throwable: Throwable? = null) : AppError(msg, throwable)
    data class PermissionError(val msg: String) : AppError(msg)
    data class NotFoundError(val msg: String) : AppError(msg)
    data class ServerError(val msg: String, val code: Int) : AppError(msg)
    data class AuthenticationError(val msg: String) : AppError(msg)
}
