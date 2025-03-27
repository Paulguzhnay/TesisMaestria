package ec.edu.ups.Backend.controller;

import ec.edu.ups.Backend.service.DockerService;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/odoo")
public class OdooController {

    @Autowired
    private DockerService dockerService;

    @PostMapping("/create")

    public Map<String, String> createInstance(@RequestBody InstanceRequest request) {
        String url = dockerService.createOdooInstance(request.getName());

        // Verificar si hubo error
        Map<String, String> response = new HashMap<>();
        if (url.startsWith("http")) {
            response.put("message", "Instancia de Odoo creada con éxito");
            response.put("url", url);  // Enviar la URL separada en la respuesta
        } else {
            response.put("message", url); // Enviar el mensaje de error
        }
        return response;
    }


    private Map<String, String> parseJsonToMap(String json) {
        Map<String, String> map = new HashMap<>();
        json = json.replaceAll("[{}\"]", ""); // Eliminar llaves y comillas
        String[] pairs = json.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                map.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return map;
    }

    static class InstanceRequest {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
