// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnExpression("${eddie.demo.button.enabled:true}")
public class DemoController {

    @GetMapping(value = "/demo", produces = MediaType.TEXT_HTML_VALUE)
    public String demo(
            Model model,
            @Value("${eddie.public.url}") String publicUrl
    ) {
        model.addAttribute("publicUrl", publicUrl);
        return "demo";
    }
}
