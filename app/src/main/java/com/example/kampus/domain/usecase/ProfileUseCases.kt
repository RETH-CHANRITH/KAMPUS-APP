package com.example.kampus.domain.usecase

import com.example.kampus.domain.model.User
import com.example.kampus.domain.repository.IUserRepository
import kotlinx.coroutines.flow.Flow

class GetCurrentUserProfileUseCase(
    private val userRepository: IUserRepository
) {
    operator fun invoke(): Flow<Result<User>> {
        return userRepository.getCurrentUserProfile()
    }
}

class GetUserStatsUseCase(
    private val userRepository: IUserRepository
) {
    operator fun invoke(userId: String): Flow<Result<User>> {
        return userRepository.getUserStats(userId)
    }
}

class GetFriendRequestsUseCase(
    private val userRepository: IUserRepository
) {
    operator fun invoke(userId: String): Flow<Result<Int>> {
        return userRepository.getFriendRequestsCount(userId)
    }
}

class AcceptFriendRequestUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> {
        return userRepository.acceptFriendRequest(requestId)
    }
}

class RejectFriendRequestUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(requestId: String): Result<Unit> {
        return userRepository.rejectFriendRequest(requestId)
    }
}

class UpdateProfileUseCase(
    private val userRepository: IUserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit> {
        return userRepository.updateProfile(user)
    }
}
