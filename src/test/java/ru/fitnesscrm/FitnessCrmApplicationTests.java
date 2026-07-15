package ru.fitnesscrm;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.utility.TestcontainersConfiguration;

@SpringBootTest(classes = FitnessCrmApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class FitnessCrmApplicationTests {
}
