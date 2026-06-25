package es.upm.fi.oeg.oops.checkers;

import static es.upm.fi.oeg.oops.Constants.LLM_IP;
import static es.upm.fi.oeg.oops.Constants.LLM_MODEL;

import dev.langchain4j.model.ollama.OllamaChatModel;
import es.upm.fi.oeg.oops.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Checker.class)
public class P17 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(17, null),
            Set.of(new PitfallCategoryId('N', 1)), Importance.IMPORTANT, "Overspecializing a hierarchy",
            "The hierarchy in the ontology is specialized in such a way "
                    + "that the final leaves are defined as classes and these classes will not have instances.\n" + "\n"
                    + "Authors in [e] provide guidelines for distinguishing between a class and an instance when modeling hierarchies.",
            RuleScope.CLASS, Arity.ONE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }

    public static String askLLM(String clase, List<String> subclases) {
        // Configuramos el modelo local
        OllamaChatModel model = OllamaChatModel.builder().baseUrl(LLM_IP).modelName("gemma3:4b").build();
        System.out.println("P17 PRUEBA" + subclases.toString());
        // Hacemos la petición
        String respuesta = model.generate(
                " You are a strict logical classifier. Your only task is to determine if a specific 'Class' in an ontology is actually a unique real-world individual. "
                        + "Rule: If the class represents a unique, specific entity that cannot have instances of its own, answer YES. If it represents a general category that can have multiple instances, answer NO. "
                        + "YOU MUST ANSWER ONLY 'YES' OR 'NO'. DO NOT ADD ANY OTHER WORDS. "
                        + "Examples: Class: 'Madrid' | Parent Class: 'City' Output: YES Class: 'European_City' | Parent Class: 'City' Output: NO Class: 'Kilimanjaro' | Parent Class: 'Mountain' Output: YES Class: 'Karst_Cave' | Parent Class: 'Cave' "
                        + "Output: NO Class:" + subclases + " | Parent Class: " + clase + " Output:");

        return respuesta;

    }

    @Override
    public void check(final CheckingContext context) {

        final OntModel model = context.getModel();

        // create lists for elements containing the pitfall
        final List<OntClass> classWithPitfall = new ArrayList<>();

        // Esto para guardar las clases q tienen subclases
        Set<OntClass> classesWithSubclasses = new HashSet<>();

        //esto es la lista de todas las clases
        List<OntClass> allClasses = model.listNamedClasses().toList();

        final Set<OntClass> involvedSet = new HashSet<>();

        for (OntClass ontoClass : allClasses) {
            // Comprobar si el iterador de subclases tiene al menos un elemento
            if (ontoClass.listSubClasses().hasNext()) {
                classesWithSubclasses.add(ontoClass);
                System.out.println("P17 Sublases añadidas: " + ontoClass.getLocalName());
            }
        }

        for (OntClass classWithSub : classesWithSubclasses) {
            List<OntClass> subClases = classWithSub.listSubClasses().toList();
            System.out.println(subClases.get(0).getLocalName());
            String name = classWithSub.getLocalName();
            List<String> subClasesNameList = new ArrayList<>();
            for (OntClass supClass : subClases) {
                String test1 = supClass.getLocalName();
                System.out.println(test1);
                subClasesNameList.add(test1);
            }

            String respuesta = askLLM(name, subClasesNameList);
            respuesta = respuesta.toLowerCase();
            respuesta = respuesta.replaceAll("\\.", "");
            respuesta = respuesta.replaceAll("\\n", "");
            System.out.println("|" + respuesta + "|");
            if (respuesta.equals("yes")) {
                classWithPitfall.add(classWithSub);
                context.addResult(PITFALL_INFO, classWithSub);

            }
        }
        // System.out.println("Results for pitfall P4. Creating unconnected ontology elements: ");
    }

}
