package carobnifrulas.web_tasks;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@WebServlet
@Push
public class WebTasksApplication implements AppShellConfigurator {

	public static void main(String[] args) {
		SpringApplication.run(WebTasksApplication.class, args);
	}

}
