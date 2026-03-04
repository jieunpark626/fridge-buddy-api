package com.fridgebuddy.fridge_buddy_server.common.init

import com.fridgebuddy.fridge_buddy_server.user.domain.User
import com.fridgebuddy.fridge_buddy_server.user.repository.UserRepository
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * MVP용 기본 사용자 생성 (user_id = 1 고정)
 * 토스 로그인 연동 시 삭제 또는 교체
 */
@Component
class DataInitializer(
    private val userRepository: UserRepository,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        if (!userRepository.existsById(1L)) {
            userRepository.save(User())
        }
    }
}