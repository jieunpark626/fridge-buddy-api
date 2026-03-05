package com.fridgebuddy.fridge_buddy_server.common.init

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/**
 * 소셜 로그인 기반으로 전환되어 MVP 초기화 로직 제거됨.
 * 사용자는 카카오/네이버 로그인 시 자동 생성됨.
 */
@Component
class DataInitializer : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        // no-op
    }
}
