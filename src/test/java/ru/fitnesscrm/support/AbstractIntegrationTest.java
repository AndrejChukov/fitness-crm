package ru.fitnesscrm.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import ru.fitnesscrm.FitnessCrmApplication;

@SpringBootTest(classes = FitnessCrmApplication.class)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("tests")
public class AbstractIntegrationTest {
}
