package com.social.wakify;

import com.wakilfly.WakilflyApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = WakilflyApplication.class)
@ActiveProfiles("test")
class WakifyApplicationTests {

	@Test
	void contextLoads() {
	}

}
