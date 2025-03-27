package ec.edu.ups.Backend.service;

import org.springframework.stereotype.Service;
import java.io.IOException;

@Service
public class OdooService {

    public boolean createOdooInstance(String name) {
        String command = "docker run -d --name " + name + " -p 8069:8069 odoo:latest";
        try {
            Process process = Runtime.getRuntime().exec(command);
            return process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
}