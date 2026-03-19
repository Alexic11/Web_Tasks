package carobnifrulas.web_tasks;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import carobnifrulas.web_tasks.card.attachment.AttachmentStorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties(AttachmentStorageProperties.class)
@WebServlet
@Push
@Theme("web-tasks")
public class WebTasksApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(WebTasksApplication.class, args);
	}

}
