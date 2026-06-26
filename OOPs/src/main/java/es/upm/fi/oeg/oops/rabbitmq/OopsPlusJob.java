package es.upm.fi.oeg.oops.rabbitmq;

public class OopsPlusJob {

    private String analysisId;
    private String ontologyPath;
    private String requestedAt;
    private boolean removeOntology;

    public OopsPlusJob() {
    }

    public OopsPlusJob(final String analysisId, final String ontologyPath, final String requestedAt) {
        this(analysisId, ontologyPath, requestedAt, false);
    }

    public OopsPlusJob(final String analysisId, final String ontologyPath, final String requestedAt,
            final boolean removeOntology) {
        this.analysisId = analysisId;
        this.ontologyPath = ontologyPath;
        this.requestedAt = requestedAt;
        this.removeOntology = removeOntology;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public void setAnalysisId(final String analysisId) {
        this.analysisId = analysisId;
    }

    public String getOntologyPath() {
        return ontologyPath;
    }

    public void setOntologyPath(final String ontologyPath) {
        this.ontologyPath = ontologyPath;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(final String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public boolean isRemoveOntology() {
        return removeOntology;
    }

    public void setRemoveOntology(final boolean removeOntology) {
        this.removeOntology = removeOntology;
    }
}
