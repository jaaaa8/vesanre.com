package com.vesanrebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Loại test: Spring context test (@SpringBootTest) - chỉ kiểm tra application context khởi động được.
 * Không gắn với API cụ thể.
 */
@SpringBootTest
class VesanreBackendApplicationTests {

	// Thành phần: Spring ApplicationContext
	// Kiểm tra: Context khởi động thành công với cấu hình/dependency hiện hành.
	@Test
	void contextLoads() {
	}

}
