package es.upm.fi.oeg.oops.checkers;

import static es.upm.fi.oeg.oops.Constants.LLM_IP;
import static es.upm.fi.oeg.oops.Constants.LLM_MODEL;

import dev.langchain4j.model.ollama.OllamaChatModel;
import es.upm.fi.oeg.oops.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.apache.jena.ontology.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Checker.class)
public class P01 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(1, null),
            Set.of(new PitfallCategoryId('N', 1), new PitfallCategoryId('S', 4), new PitfallCategoryId('N', 7)),
            Importance.CRITICAL, "Creating polysemous elements",
            "An ontology element (class, object property or datatype property) whose identifier has different senses is "
                    + "included in the ontology to represent more than one conceptual idea or property.",
            RuleScope.CLASS, Arity.ONE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }

    public static String askLLM(String clase, List<String> subclases) {
        // Configuramos el modelo local
        OllamaChatModel model = OllamaChatModel.builder().baseUrl(LLM_IP).modelName(LLM_MODEL).build();
        System.out.println("P01 PRUEBA" + subclases.toString());
        // Hacemos la petición
        String respuesta = model
                .generate(" Task: Determine if the word" + clase + " represents both concepts listed below.Concepts:"
                        + subclases.toString() + ".Output strictly 'Yes' or 'No'.");

        return respuesta;

    }

    @Override
    public void check(final CheckingContext context) {

        final OntModel model = context.getModel();

        // create lists for elements containing the pitfall
        final List<OntClass> classWithPitfall = new ArrayList<>();

        // Esto para guardar las clases q tienen subclases
        Set<OntClass> classesWithSuperclasses = new HashSet<>();

        //esto es la lista de todas las clases
        List<OntClass> allClasses = model.listNamedClasses().toList();

        final Set<OntClass> involvedSet = new HashSet<>();

        for (OntClass ontoClass : allClasses) {
            // Comprobar si el iterador de subclases tiene al menos un elemento
            if (ontoClass.listSuperClasses().hasNext()) {
                classesWithSuperclasses.add(ontoClass);
                System.out.println("P01 Clases añadidas: " + ontoClass.getLocalName());
            }
        }

        for (OntClass classWithSup : classesWithSuperclasses) {
            List<OntClass> superClases = classWithSup.listSuperClasses().toList();
            System.out.println(superClases.get(0).getLocalName());
            String name = classWithSup.getLocalName();
            List<String> supClasesNameList = new ArrayList<>();
            for (OntClass supClass : superClases) {
                String test1 = supClass.getLocalName();
                System.out.println(test1);
                supClasesNameList.add(test1);
            }

            String respuesta = askLLM(name, supClasesNameList);
            respuesta = respuesta.toLowerCase();
            respuesta = respuesta.replaceAll("\\.", "");
            respuesta = respuesta.replaceAll("\\n", "");
            System.out.println("|" + respuesta + "|");
            if (respuesta.equals("yes")) {
                //HOLA
                System.out.println("DENTRO");
                classWithPitfall.add(classWithSup);
                context.addResult(PITFALL_INFO, classWithSup);

            }
        }
        // System.out.println("Results for pitfall P4. Creating unconnected ontology elements: ");
    }

}
