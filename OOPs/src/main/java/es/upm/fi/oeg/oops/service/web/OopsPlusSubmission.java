package es.upm.fi.oeg.oops.service.web;

import java.nio.file.Path;

public class OopsPlusSubmission {

    private final String analysisId;
    private final Path ontologyPath;
    private final Path statusPath;

    public OopsPlusSubmission(final String analysisId, final Path ontologyPath, final Path statusPath) {
        this.analysisId = analysisId;
        this.ontologyPath = ontologyPath;
        this.statusPath = statusPath;
    }

    public String getAnalysisId() {
        return analysisId;
    }

    public Path getOntologyPath() {
        return ontologyPath;
    }

    public Path getStatusPath() {
        return statusPath;
    }
}
