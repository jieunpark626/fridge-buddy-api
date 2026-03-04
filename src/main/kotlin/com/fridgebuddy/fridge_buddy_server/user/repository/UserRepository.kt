package com.fridgebuddy.fridge_buddy_server.user.repository

import com.fridgebuddy.fridge_buddy_server.user.domain.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>