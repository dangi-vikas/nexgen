package com.nexgen.order_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Requires SQL Server, Redis and Kafka. Enable when Docker/Testcontainers integration infrastructure is available.")
@SpringBootTest
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
