package es.upm.fi.oeg.oops.workers;

import es.upm.fi.oeg.oops.Importance;
import es.upm.fi.oeg.oops.Linter;
import es.upm.fi.oeg.oops.Pitfall;
import es.upm.fi.oeg.oops.PitfallId;
import es.upm.fi.oeg.oops.PitfallInfo;
import es.upm.fi.oeg.oops.Report;
import es.upm.fi.oeg.oops.RuleScope;
import es.upm.fi.oeg.oops.Utils;
import es.upm.fi.oeg.oops.Warning;
import es.upm.fi.oeg.oops.WarningType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

public class OopsPlusReportWriter {

    public void writeHtml(final Path reportPath, final Report report) throws IOException {
        Files.writeString(reportPath, toHtml(report), StandardCharsets.UTF_8);
    }

    private String toHtml(final Report report) {
        final StringBuilder html = new StringBuilder();
        final Map<PitfallId, List<Pitfall>> pitfalls = report.getPitfalls();
        final Model outputModel = report.getOutputModel();
        final Property affectedProperty = outputModel.getProperty(Linter.NS_OOPS_DEF + "hasAffectedElement");

        if (pitfalls.isEmpty()) {
            appendNoPitfallsHtml(html);
            return html.toString();
        }

        appendImportanceLegend(html);
        html.append("<h3>Pitfalls detected:</h3>\n");
        html.append("<br>\n");
        html.append("<div class=\"accordion\" id=\"accordionPanelsStayOpenExample\">\n");

        Importance highestTriggeredImportance = null;
        final List<Map.Entry<PitfallId, List<Pitfall>>> pitfallEntries = new ArrayList<>(pitfalls.entrySet());
        pitfallEntries.sort(Comparator.comparing(Map.Entry::getKey, this::comparePitfallIds));

        for (final Map.Entry<PitfallId, List<Pitfall>> pfEntry : pitfallEntries) {
            final PitfallId pfId = pfEntry.getKey();
            final List<Pitfall> pfs = pfEntry.getValue();
            final PitfallInfo info = pfs.get(0).getInfo();
            final Importance importance = info.getImportance();
            if (highestTriggeredImportance == null || highestTriggeredImportance.compareTo(importance) < 0) {
                highestTriggeredImportance = importance;
            }

            appendPitfallAccordion(html, pfId, info, pfs, affectedProperty);
        }

        html.append("</div>\n");
        appendWarningsHtml(html, report);
        appendConformanceBadgeHtml(html, highestTriggeredImportance);
        return html.toString();
    }

    private void appendNoPitfallsHtml(final StringBuilder html) {
        html.append("<div class=\"txt\">\n");
        html.append("<h1>Congratulations! No pitfalls detected.</h1>\n");
        html.append("<br>\n");
        html.append("After scanning your ontology with OOPS! we could not find any pitfall. ");
        html.append("However, remember that there are pitfalls that depend on the application requirements and OOPS! ");
        html.append("is not able to detect them automatically.\n");
        html.append("<br><br>\n");
        html.append("<a href=\"index.jsp\"><img src=\"images/conformance/oops_free.png\" style=\"float:initial;\" ");
        html.append("alt=\"Free of pitfalls\" height=\"69.6\" width=\"100\" /></a>\n");
        html.append("<br>\n");
        html.append("You can use the following HTML code:<br><br>\n");
        html.append("<pre><code>&lt;a href=\"http://oops.linkeddata.es\"&gt;&lt;img <br>\n");
        html.append("src=\"images/conformance/oops_free.png\"<br>\n");
        html.append("alt=\"Free of pitfalls\" height=\"69.6\" width=\"100\" /&gt;</code></pre>\n");
        html.append("</div>\n");
    }

    private int comparePitfallIds(final PitfallId first, final PitfallId second) {
        final int numeralComparison = Integer.compare(first.getNumeral(), second.getNumeral());
        if (numeralComparison != 0) {
            return numeralComparison;
        }
        return comparePitfallFlavors(first.getFlavor(), second.getFlavor());
    }

    private int comparePitfallFlavors(final Character first, final Character second) {
        if (first == null && second == null) {
            return 0;
        }
        if (first == null) {
            return -1;
        }
        if (second == null) {
            return 1;
        }
        return Character.compare(first, second);
    }

    private void appendImportanceLegend(final StringBuilder html) {
        html.append("<br>\n");
        html.append("<div class=\"txt\">\n");
        html.append("There are three levels of importance in pitfalls according to their impact on the ontology:\n");
        html.append("<ul>\n");
        html.append("<li><span class=\"badge bg-danger\" >Critical</span> ");
        html.append("It is crucial to correct the pitfall. Otherwise, it could affect the ontology consistency, ");
        html.append("reasoning, applicability, etc.</li>\n");
        html.append("<li><span class=\"badge\" style=\"background-color: #ff8000 !important;\">Important</span> ");
        html.append(
                "Though not critical for ontology function, it is important to correct this type of pitfall.</li>\n");
        html.append("<li><span class=\"badge bg-warning\" >Minor</span> ");
        html.append("It is not really a problem, but by correcting it we will make the ontology nicer.</li>\n");
        html.append("</ul>\n");
        html.append("</div>\n");
        html.append("<br>\n");
    }

    private void appendPitfallAccordion(final StringBuilder html, final PitfallId pfId, final PitfallInfo info,
            final List<Pitfall> pfs, final Property affectedProperty) {
        final String title = Utils.escapeForHtml(info.getTitle());
        final String explanation = Utils.escapeForHtml(info.getExplanation());
        final String scopeText = scopeText(info, pfs);

        html.append("<div class=\"accordion-item\">\n");
        html.append("<h2 class=\"accordion-header\" id=\"panelsStayOpen-heading").append(pfId).append("\">\n");
        html.append("<button class=\"accordion-button collapsed\" style=\"display:inline;\" type=\"button\" ");
        html.append("data-bs-toggle=\"collapse\" data-bs-target=\"#panelsStayOpen-collapse").append(pfId);
        html.append("\" aria-expanded=\"false\" aria-controls=\"panelsStayOpen-collapse").append(pfId).append("\">\n");
        html.append("Results for ").append(pfId).append(": ").append(title).append(".\n");
        html.append(importanceBadgeHtml(info.getImportance()));
        html.append("<span style=\"float: right; padding-right:15px;\">").append(scopeText).append("</span>\n");
        html.append("</button>\n");
        html.append("</h2>\n");
        html.append("<div id=\"panelsStayOpen-collapse").append(pfId).append("\" ");
        html.append("class=\"accordion-collapse collapse\" aria-labelledby=\"panelsStayOpen-heading").append(pfId);
        html.append("\">\n");
        html.append("<div class=\"accordion-body\">\n");
        html.append(explanation).append("\n<br>\n");

        if (info.getScope() == RuleScope.ONTOLOGY) {
            html.append("<br>\n*This pitfall applies to the ontology in general instead of specific elements.\n<br>\n");
        } else {
            appendPitfallResources(html, info, pfs, affectedProperty);
        }

        html.append("</div>\n");
        html.append("</div>\n");
        html.append("</div>\n");
    }

    private String importanceBadgeHtml(final Importance importance) {
        switch (importance) {
            case CRITICAL :
                return "<span class=\"badge bg-danger\" style=\"float: right;\">Critical</span>";
            case IMPORTANT :
                return "<span class=\"badge\" style=\"background-color: #ff8000 !important;float: right;\">Important</span>";
            case MINOR :
                return "<span class=\"badge bg-warning\" style=\"float: right;\">Minor</span>";
            default :
                throw new IllegalStateException();
        }
    }

    private String scopeText(final PitfallInfo info, final List<Pitfall> pfs) {
        if (info.getScope() == RuleScope.ONTOLOGY) {
            return "Ontology*";
        }
        if (pfs.size() == 1) {
            return "1 case";
        }
        return pfs.size() + " cases";
    }

    private void appendPitfallResources(final StringBuilder html, final PitfallInfo info, final List<Pitfall> pfs,
            final Property affectedProperty) {
        if (!isCompound(pfs, affectedProperty)) {
            html.append("<br>\n&bull; ");
            html.append(Utils.escapeForHtml(
                    info.getAccomp() == null ? "This pitfall appears in the following elements" : info.getAccomp()));
            html.append(":\n<br>\n");
        }

        for (final Pitfall pf : pfs) {
            for (final Resource res : pf.getResources()) {
                if (affectedProperty != null && res.hasProperty(affectedProperty)) {
                    html.append(" <br>&bull; ").append(Utils.escapeForHtml(info.getAccomp())).append(":<br>");
                    for (final Statement stmt : res.listProperties(affectedProperty).toList()) {
                        final RDFNode obj = stmt.getObject();
                        if (obj.isResource()) {
                            html.append(" &nbsp;&nbsp;&nbsp;&rsaquo; ").append(asLink(obj.asResource())).append("<br>");
                        } else {
                            html.append("<i>unknown</i><br>");
                        }
                    }
                } else {
                    html.append(" &rsaquo; ").append(asLink(res)).append("\n<br>\n");
                }
            }
        }
    }

    private boolean isCompound(final List<Pitfall> pitfalls, final Property affectedProperty) {
        if (affectedProperty == null) {
            return false;
        }
        for (final Pitfall pf : pitfalls) {
            for (final Resource res : pf.getResources()) {
                if (res.hasProperty(affectedProperty)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String asLink(final Resource res) {
        final String uri = res.isURIResource() ? res.getURI() : res.toString();
        final String label = res.isURIResource() && res.getLocalName() != null ? res.getLocalName() : uri;
        return String.format("<a href=\"%s\" target=\"_blank\">%s</a>", Utils.escapeForHtml(uri),
                Utils.escapeForHtml(label));
    }

    private void appendWarningsHtml(final StringBuilder html, final Report report) {
        if (report.getWarnings().isEmpty()) {
            return;
        }

        html.append("<br>\n<h3>Warnings:</h3>\n<br>\n");
        html.append("<div class=\"accordion\" id=\"accordionPanelsStayOpenExample\">\n");

        final List<Map.Entry<WarningType, List<Warning>>> warningEntries = new ArrayList<>(
                report.getWarnings().entrySet());
        warningEntries.sort(Comparator.comparingInt(entry -> warningTypeDisplayOrder(entry.getKey())));
        for (final Map.Entry<WarningType, List<Warning>> wEntry : warningEntries) {
            final WarningType wType = wEntry.getKey();
            final List<Warning> wTWarnings = wEntry.getValue();
            html.append("<div class=\"accordion-item\">\n");
            html.append("<h2 class=\"accordion-header\" id=\"panelsStayOpen-headingSug\">\n");
            html.append("<button class=\"accordion-button collapsed\" type=\"button\" data-bs-toggle=\"collapse\" ");
            html.append("data-bs-target=\"#panelsStayOpen-collapseSug\" aria-expanded=\"false\" ");
            html.append("aria-controls=\"panelsStayOpen-collapseSug\">\n");
            html.append(wType).append(" related warnings\n");
            html.append("<span style=\"float: right; \">| ").append(wTWarnings.size()).append(" case");
            if (wTWarnings.size() > 1) {
                html.append("s");
            }
            html.append("</span>\n</button>\n</h2>\n");
            html.append("<div id=\"panelsStayOpen-collapseSug\" class=\"accordion-collapse collapse\" ");
            html.append("aria-labelledby=\"panelsStayOpen-headingSug\">\n<div class=\"accordion-body\">\n");

            for (final Warning warning : wTWarnings) {
                html.append(Utils.escapeForHtml(warning.toString()));
                html.append("<br><br>\nOrigin: Checker ").append(warning.getCheckerInfo().getId()).append("<br>\n");
                final Set<OntResource> scope = warning.getScope();
                if (!scope.isEmpty()) {
                    html.append("<ul>\n");
                    for (final OntResource wRes : scope) {
                        html.append("<li>").append(asLink(wRes)).append("</li>\n");
                    }
                    html.append("</ul>\n");
                }
            }

            html.append("</div>\n</div>\n</div>\n");
        }

        html.append("</div>\n");
    }

    private int warningTypeDisplayOrder(final WarningType warningType) {
        switch (warningType) {
            case CLASS :
                return 0;
            case ONTOLOGY :
                return 1;
            case PROPERTY :
                return 2;
            default :
                return 3;
        }
    }

    private void appendConformanceBadgeHtml(final StringBuilder html, final Importance highestTriggeredImportance) {
        if (highestTriggeredImportance == null) {
            return;
        }

        final String importanceName = highestTriggeredImportance.name().toLowerCase();
        html.append("<br>\n<br>\n<div class=txt>\n");
        html.append("<p>According to the highest importance level of pitfall found in your ontology, ");
        html.append("the conformance badge suggested is \"").append(highestTriggeredImportance);
        html.append(" pitfalls\" (see below). You can use the following HTML code to insert ");
        html.append("the badge within your ontology documentation:</p>\n");
        html.append("<div class=codeLogo>\n");
        html.append("<a href=\"http://oops.linkeddata.es\"><img src=\"images/conformance/oops_");
        html.append(importanceName).append(".png\" alt=\"").append(highestTriggeredImportance);
        html.append(" pitfalls were found\" height=\"69.6\" width=\"100\" /></a><br>\n");
        html.append("<div class=code>\n<pre>\n");
        html.append("&lt;p&gt;\n&lt;a href=\"http://oops.linkeddata.es\"&gt;\n");
        html.append("&lt;img src=\"http://oops.linkeddata.es/resource/image/oops_").append(importanceName);
        html.append(".png\"\nalt=\"").append(highestTriggeredImportance);
        html.append(" pitfalls were found\" height=\"69.6\" width=\"100\" /&gt;&lt;/a&gt;\n&lt;/p&gt;</pre>\n");
        html.append("</div>\n</div>\n</div>\n");
        html.append("<br>\n<br>\n");
    }
}
