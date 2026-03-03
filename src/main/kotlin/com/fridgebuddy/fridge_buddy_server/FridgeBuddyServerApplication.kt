package com.fridgebuddy.fridge_buddy_server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FridgeBuddyServerApplication

fun main(args: Array<String>) {
	runApplication<FridgeBuddyServerApplication>(*args)
}
