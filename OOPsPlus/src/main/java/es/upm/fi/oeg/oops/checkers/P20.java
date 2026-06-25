/*
 * SPDX-FileCopyrightText: 2014 María Poveda Villalón <mpovedavillalon@gmail.com>
 * SPDX-FileCopyrightText: 2025 Robin Vobruba <hoijui.quaero@gmail.com>
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package es.upm.fi.oeg.oops.checkers;

import es.upm.fi.oeg.oops.Arity;
import es.upm.fi.oeg.oops.Checker;
import es.upm.fi.oeg.oops.CheckerInfo;
import es.upm.fi.oeg.oops.CheckingContext;
import es.upm.fi.oeg.oops.ExtIterIterable;
import es.upm.fi.oeg.oops.Importance;
import es.upm.fi.oeg.oops.PitfallCategoryId;
import es.upm.fi.oeg.oops.PitfallId;
import es.upm.fi.oeg.oops.PitfallInfo;
import es.upm.fi.oeg.oops.RuleScope;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.kohsuke.MetaInfServices;

@MetaInfServices(Checker.class)
public class P20 implements Checker {

    private static final PitfallInfo PITFALL_INFO = new PitfallInfo(new PitfallId(20, null),
            Set.of(new PitfallCategoryId('N', 7)), Importance.MINOR, "Misusing ontology annotations",
            "The contents of some annotation properties are swapped or misused. "
                    + "This pitfall might affect annotation properties related to natural language information "
                    + "(for example, annotations for naming such as rdfs:label "
                    + "or for providing descriptions such as rdfs:comment). "
                    + "Other types of annotation could also be affected; "
                    + "temporal and versioning information, among others.",
            RuleScope.RESOURCE, Arity.ONE);

    public static final CheckerInfo INFO = new CheckerInfo(PITFALL_INFO);

    private OntProperty skosLabel;
    private OntProperty skosDef;
    private OntProperty skosAltLabel;
    private OntProperty dctDesc;
    private OntProperty oboDef;

    public P20() {
        final OntModel annotations = ModelFactory.createOntologyModel();
        annotations.createObjectProperty("http://www.w3.org/2004/02/skos/core#definition");
        annotations.createObjectProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
        annotations.createObjectProperty("https://www.w3.org/2009/08/skos-reference/skos.html#altLabel");

        annotations.createObjectProperty("http://purl.org/dc/terms/description");

        this.skosDef = annotations.getOntProperty("http://www.w3.org/2004/02/skos/core#definition");
        this.skosLabel = annotations.getOntProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
        this.skosAltLabel = annotations.getOntProperty("https://www.w3.org/2009/08/skos-reference/skos.html#altLabel");

        this.dctDesc = annotations.getOntProperty("http://purl.org/dc/terms/description");
        this.oboDef = annotations.getOntProperty("http://purl.obolibrary.org/obo/IAO_0000118");

    }
    @Override
    public CheckerInfo getInfo() {
        return INFO;
    }

    @Override
    public void check(final CheckingContext context) {
        scanResources(context, () -> context.getModel().listNamedClasses());
        scanResources(context, () -> context.getModel().listObjectProperties());
        scanResources(context, () -> context.getModel().listDatatypeProperties());
    }

    private <PT extends OntResource> void scanResources(final CheckingContext context,
            final Supplier<ExtendedIterator<PT>> allResGen) {

        // create lists for elements containing the pitfall
        // List<OntClass> classWithPitfall = new ArrayList<OntClass>();

        for (final PT ontoRes : new ExtIterIterable<>(allResGen.get())) {
            // try {
            //HE QUITADO FINAL
            String label = ontoRes.getLabel(null);
            String comment = ontoRes.getComment(null);

            //skos prefLabel
            String skosPref = null;
            if (ontoRes.getPropertyValue(skosLabel) != null) {
                skosPref = ontoRes.getPropertyValue(skosLabel).toString();
            }

            //skos AltaLabel
            String skosAlt = null;
            if (ontoRes.getPropertyValue(skosAltLabel) != null) {
                skosAlt = ontoRes.getPropertyValue(skosAltLabel).toString();
            }

            //obo
            String obo = null;
            if (ontoRes.getPropertyValue(oboDef) != null) {
                obo = ontoRes.getPropertyValue(oboDef).toString();
            }

            //skos definition
            String skosDefinition = null;
            if (ontoRes.getPropertyValue(skosDef) != null) {
                skosDefinition = ontoRes.getPropertyValue(skosDef).toString();
            }

            //dc description por coherencia??
            String dctDescription = null;
            if (ontoRes.getPropertyValue(dctDesc) != null) {
                dctDescription = ontoRes.getPropertyValue(dctDesc).toString();
            }

            System.out.println("P20 skosPrefLabel" + skosPref);
            System.out.println("P20 label" + label);
            System.out.println("P20 comment" + comment);

            if (label == null) {
                if (skosPref != null) {
                    label = skosPref;
                }
                if (skosAlt != null) {
                    label = skosAlt;
                }
                if (obo != null) {
                    label = obo;
                }
            }

            if (comment == null) {
                if (skosDefinition != null) {
                    comment = skosDefinition;
                }
                if (dctDescription != null) {
                    comment = dctDescription;
                }
            }
            //CAMBIAR ESTO??

            if ((label != null) && (comment != null)) {
                boolean pitfall = false;
                if (label.isEmpty() || comment.isEmpty()) {
                    pitfall = true;
                    // System.out.println("el comment o el label esta vacio");
                    // System.out.println("                Comment: " + comment);
                    // System.out.println("                Label: " + label);
                } else if ((label.split(" ").length > comment.split(" ").length) && !Checker.fromModels(ontoRes)) {
                    pitfall = true;
                    // } else if ((label.length() > comment.length()) && !Checker.fromModels(ontoClass)) {
                    // pitfall = true;
                } else if (label.equalsIgnoreCase(comment)) {
                    // if they have the same content
                    pitfall = true;
                }
                if (pitfall) {
                    // if (comment.split(" ").length == 1) {
                    // System.out.println(propertyWithPitfall.get(numPWithPitfall).getURI() + " It is a one-word
                    // comment, maybe it is an annotation about the status");
                    // }
                    context.addResult(PITFALL_INFO, ontoRes);
                }
            }
            // } catch (final LiteralRequiredException exc) {
            // System.err.println("DETECTADA: " + exc.getMessage());
            // }
        }
    }
}
