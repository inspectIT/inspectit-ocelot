package rocks.inspectit.oce.eum.server.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
@Slf4j
public class ConfigurationEditorController {

    @Autowired
    private Environment environment;


    @GetMapping("configuration")
    public String getConfigurationEditor(Model model) {
        String port = environment.getProperty("local.server.port");
        model.addAttribute("port", port);
        return "configuration";
    }
}
