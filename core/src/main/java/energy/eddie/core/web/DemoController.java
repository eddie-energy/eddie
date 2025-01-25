package energy.eddie.core.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnExpression("${eddie.demo.button.enabled:true}")
public class DemoController {

    @GetMapping(value = "/demo", produces = MediaType.TEXT_HTML_VALUE)
    public String demo() {
        return "demo";
    }
}
