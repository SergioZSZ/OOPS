package es.upm.fi.oeg.oops.checkers;

import static es.upm.fi.oeg.oops.Constants.LLM_IP;
import static es.upm.fi.oeg.oops.Constants.LLM_MODEL;

import dev.langchain4j.model.ollama.OllamaChatModel;
import es.upm.fi.oeg.oops.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Checker.class)
public class P23 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(23, null),
            Set.of(new PitfallCategoryId('N', 1)), Importance.IMPORTANT,
            "Duplicating a datatype already provided by the implementation language",
            "A class and its corresponding individuals are created to represent existing datatypes in the implementation language.",
            RuleScope.CLASS, Arity.ONE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }

    public final String[] primitiveDatatypes = { "string", "boolean", "decimal", "float", "double", "duration",
            "dateTime", "time", "date", "gYearMonth", "gYear", "gMonthDay", "gDay", "gMonth", "hexBinary",
            "base64Binary", "anyURI", "QName", "NOTATION" };

    public final String[] derivedDatatypes = { "normalizedString", "token", "language", "Name", "NCName", "ID", "IDREF",
            "IDREFS", "ENTITY", "ENTITIES", "NMTOKEN", "NMTOKENS", "integer", "nonPositiveInteger", "negativeInteger",
            "nonNegativeInteger", "positiveInteger", "long", "int", "short", "byte", "unsignedLong", "unsignedInt",
            "unsignedShort", "unsignedByte" };
    @Override
    public void check(final CheckingContext context) {

        final OntModel model = context.getModel();

        List<OntClass> allClasses = model.listNamedClasses().toList();

        for (OntClass ontoclass : allClasses) {
            Boolean isData = false;
            String localName = ontoclass.getLocalName();
            localName = localName.toLowerCase();
            if (localName == null || localName.isEmpty())
                continue;

            for (int i = 0; i < primitiveDatatypes.length; i++) {
                if (localName.equals(primitiveDatatypes[i])) {
                    isData = true;
                    break;
                }
            }
            if (!isData) {
                for (int j = 0; j < derivedDatatypes.length; j++) {
                    if (localName.equals(derivedDatatypes[j])) {
                        isData = true;
                        break;
                    }
                }
            }

            if (isData && ontoclass.listInstances().hasNext()) {
                context.addResult(PITFALL_INFO, ontoclass);

            }

        }
    }
}
