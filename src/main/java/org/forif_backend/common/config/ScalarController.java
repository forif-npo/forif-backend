package org.forif_backend.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ScalarController {

    @GetMapping("/scalar")
    @ResponseBody
    public String scalar() {
        return """
                <!doctype html>
                <html>
                <head>
                    <title>Forif API Docs</title>
                    <meta charset="utf-8" />
                    <meta name="viewport" content="width=device-width, initial-scale=1" />
                </head>
                <body>
                    <script
                        id="api-reference"
                        data-url="/v3/api-docs">
                    </script>
                    <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
                </body>
                </html>
                """;
    }
}
