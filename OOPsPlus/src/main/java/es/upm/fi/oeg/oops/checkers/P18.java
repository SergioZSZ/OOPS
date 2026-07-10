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
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Checker.class)
public class P18 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(18, null),
            Set.of(new PitfallCategoryId('N', 2), new PitfallCategoryId('S', 4)), Importance.IMPORTANT,
            "Overspecializing the domain or range",
            "This pitfall consists in defining a domain or range not general enough for a property, "
                    + "i.e no considering all the individuals or datatypes that might be involved in such a domain or range.\n"
                    + "\n" + "This pitfall is related to the guidelines provided in [2] and [7].",
            RuleScope.CLASS, Arity.ONE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }

    public static String askDomLLM(String property, String supClass, String clase) {
        // Configuramos el modelo local
        OllamaChatModel model = OllamaChatModel.builder().baseUrl(LLM_IP).modelName(LLM_MODEL)
                .timeout(Duration.ofMinutes(10)).build();
        System.out.println("P18 PRUEBA Domain relacion" + property + "supCLass" + supClass + "class" + clase);
        // Hacemos la petición
        String respuesta = model.generate(" Task: Determine if the class " + supClass
                + " could serve as subject of the property " + property + ".Output strictly 'Yes' or 'No'.");

        return respuesta;

    }
    public static String askRanLLM(String property, String supClass, String clase) {
        // Configuramos el modelo local
        OllamaChatModel model = OllamaChatModel.builder().baseUrl(LLM_IP).modelName(LLM_MODEL)
                .timeout(Duration.ofMinutes(10)).build();
        System.out.println("P18 PRUEBA Range relacion" + property + "supCLass" + supClass + "class" + clase);
        // Hacemos la petición
        String respuesta = model.generate(" Task: Determine if the class " + supClass
                + " could serve as object of the property " + property + ".Output strictly 'Yes' or 'No'.");

        return respuesta;

    }

    @Override
    public void check(final CheckingContext context) {

        final OntModel model = context.getModel();
        final Set<OntProperty> propertiesToCheck = new HashSet<>();

        ExtendedIterator<ObjectProperty> objProps = model.listObjectProperties();
        while (objProps.hasNext()) {
            propertiesToCheck.add(objProps.next());
        }

        //ExtendedIterator<DatatypeProperty> dataProps = model.listDatatypeProperties();
        //while (dataProps.hasNext()) {
        //  propertiesToCheck.add(dataProps.next());
        //}

        for (OntProperty prop : propertiesToCheck) {
            if (prop.getLocalName() == null)
                continue;

            //mirar dominio
            OntResource domain = prop.getDomain();

            if (domain != null && domain.isClass()) {
                OntClass domainClass = domain.asClass();

                if (domainClass.isURIResource()
                        && !domainClass.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) {
                    OntClass supClass = domainClass.getSuperClass();
                    if (supClass == null)
                        continue;
                    if (supClass.isURIResource() && !supClass.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) {
                        String respuesta1 = askDomLLM(prop.getLocalName(), supClass.getLocalName(),
                                domainClass.getLocalName());
                        respuesta1 = respuesta1.toLowerCase();
                        respuesta1 = respuesta1.replaceAll("\\.", "");
                        respuesta1 = respuesta1.replaceAll("\\n", "");
                        System.out.println("|" + respuesta1 + "|");
                        if (respuesta1.equals("yes")) {
                            context.addResult(PITFALL_INFO, prop);
                        }
                    }
                }
            }

            OntResource range = prop.getRange();

            if (range != null && range.isClass()) {
                OntClass rangeClass = range.asClass();

                if (rangeClass.isURIResource() && !rangeClass.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) {
                    OntClass supClass = rangeClass.getSuperClass();
                    if (supClass == null)
                        continue;
                    if (supClass.isURIResource() && !supClass.getURI().equals("http://www.w3.org/2002/07/owl#Thing")) {
                        String respuesta2 = askRanLLM(prop.getLocalName(), supClass.getLocalName(),
                                rangeClass.getLocalName());
                        respuesta2 = respuesta2.toLowerCase();
                        respuesta2 = respuesta2.replaceAll("\\.", "");
                        respuesta2 = respuesta2.replaceAll("\\n", "");
                        System.out.println("|" + respuesta2 + "|");
                        if (respuesta2.equals("yes")) {
                            context.addResult(PITFALL_INFO, prop);
                        }
                    }
                }
            }

        }

    }
    // System.out.println("Results for pitfall P4. Creating unconnected ontology elements: ");

}
