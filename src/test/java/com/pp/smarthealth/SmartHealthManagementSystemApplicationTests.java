package com.pp.smarthealth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartHealthManagementSystemApplicationTests {

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.pp.smarthealth.config.ScheduledTasks scheduledTasks;

	@Test
	void contextLoads() {
	}

}
