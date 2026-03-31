window.onload = function () {
    window.ui = SwaggerUIBundle({
        configUrl: "/examind/API/api-docs/swagger-config",
        dom_id: '#swagger-ui',
        presets: [
            SwaggerUIBundle.presets.apis,
            SwaggerUIStandalonePreset
        ],
        layout: "StandaloneLayout"
    })
}
