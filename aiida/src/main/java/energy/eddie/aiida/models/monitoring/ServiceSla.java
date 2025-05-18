package energy.eddie.aiida.models.monitoring;

import java.util.List;

public class ServiceSla {
    private String name;
    private List<ServiceSlo> slos;

    public ServiceSla(String name, List<ServiceSlo> slos) {
        this.name = name;
        this.slos = slos;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ServiceSlo> getSlos() {
        return slos;
    }

    public void setSlos(List<ServiceSlo> slos) {
        this.slos = slos;
    }
}


