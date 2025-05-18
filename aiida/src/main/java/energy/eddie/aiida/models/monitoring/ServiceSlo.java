package energy.eddie.aiida.models.monitoring;

import energy.eddie.aiida.models.monitoring.openslo.alert.AlertPolicyDocument;
import energy.eddie.aiida.models.monitoring.openslo.sli.SliDocument;
import energy.eddie.aiida.models.monitoring.openslo.slo.SloDocument;

public class ServiceSlo {
    private SloDocument slo;
    private SliDocument sli;
    private AlertPolicyDocument alertPolicy;

    public ServiceSlo(SloDocument slo, SliDocument sli, AlertPolicyDocument alertPolicy) {
        this.slo = slo;
        this.sli = sli;
        this.alertPolicy = alertPolicy;
    }

    public SloDocument getSlo() {
        return slo;
    }

    public SliDocument getSli() {
        return sli;
    }

    public AlertPolicyDocument getAlertPolicy() {
        return alertPolicy;
    }
}
