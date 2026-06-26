package es.upm.fi.oeg.oops.checkers;

import static es.upm.fi.oeg.oops.Constants.LLM_IP;
import static es.upm.fi.oeg.oops.Constants.LLM_MODEL;

import dev.langchain4j.model.ollama.OllamaChatModel;
import es.upm.fi.oeg.oops.*;
import java.util.*;
import org.apache.jena.ontology.AllValuesFromRestriction;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Checker.class)
public class P14 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(14, null),
            Set.of(new PitfallCategoryId('N', 1), new PitfallCategoryId('S', 4)), Importance.CRITICAL,
            "Misusing \"owl:allValuesFrom\"",
            "This pitfall consists in using the universal restriction (owl:allValuesFrom) as the default qualifier "
                    + "instead of the existential restriction (owl:someValuesFrom).\n" + "\n"
                    + "Additional information about this pitfall is provided in [7].",
            RuleScope.CLASS, Arity.ONE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }

    public static String askLLM(String classA, String prop, String classB) {
        // Configuramos el modelo local
        OllamaChatModel model = OllamaChatModel.builder().baseUrl(LLM_IP).modelName(LLM_MODEL).build();
        // Hacemos la petición
        String respuesta = model.generate("Is it possible to have individuals that do not belong to class " + classA
                + " not to " + prop + " " + classB + " ? Answer strictly 'Yes' or 'No'.");

        return respuesta;

    }

    @Override
    public void check(final CheckingContext context) {

        final OntModel model = context.getModel();

        // create lists for elements containing the pitfall
        final List<OntClass> classWithPitfall = new ArrayList<>();

        // Esto para guardar las clases q tienen subclases
        Set<OntClass> allValuesFromClasses = new HashSet<>();

        //esto es la lista de todas las clases
        List<OntClass> allClasses = model.listNamedClasses().toList();
        System.out.println("HOLA P14");
        ExtendedIterator<Restriction> list = model.listRestrictions();
        System.out.println("Lista creada  ");

        if (list.hasNext()) {
            System.out.println("HOLA dentro de bucle");

            Restriction r1 = list.next();
            System.out.println("IS AVF? " + r1.isAllValuesFromRestriction());
            if (r1.isAllValuesFromRestriction()) {
                AllValuesFromRestriction avf = r1.asAllValuesFromRestriction();
                String prop = avf.getOnProperty().getLocalName();
                System.out.println("ONprop PROPIEDAD " + prop);

                String classB = avf.getAllValuesFrom().getLocalName();
                System.out.println("AVF " + classB);

                //if (avf.getSubClass() != null) {
                String classA = avf.getSubClass().getLocalName();
                System.out.println("SUB " + classA);
                //}

                String respuesta = askLLM(classA, prop, classB);
                System.out.println("P14 " + respuesta);
                respuesta = respuesta.toLowerCase();
                respuesta = respuesta.replaceAll("\\.", "");
                respuesta = respuesta.replaceAll("\\n", "");
                System.out.println("|" + respuesta + "|");
                if (respuesta.equals("yes")) {
                    //REVISAR
                    context.addResult(PITFALL_INFO, avf.getSubClass());

                }

            }
        }

    }
}
