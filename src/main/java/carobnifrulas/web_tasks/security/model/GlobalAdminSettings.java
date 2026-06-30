package carobnifrulas.web_tasks.security.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GlobalAdminSettings {

    public GlobalAdminSettings(@Value("${app.security.global-admin-email:admin@local}") String globalAdminEmail) {
        SecurityUtils.setGlobalAdminEmail(globalAdminEmail);
    }
}
