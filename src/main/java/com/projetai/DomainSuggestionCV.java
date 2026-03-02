package com.projetai;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;
import weka.core.stopwords.Rainbow;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DomainSuggestionCV {

    private static Classifier model;
    private static Instances filteredData;
    private static ArrayList<Attribute> attributes;
    private static StringToWordVector filter;

    // Méthode d'initialisation : charger CSV et entraîner le modèle
    public static void init() throws Exception {
        List<String> textes = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        try (InputStream is = DomainSuggestionCV.class.getResourceAsStream("/csv/domaines.csv")) {
            if (is == null) {
                throw new Exception("Fichier CSV introuvable !");
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            br.readLine(); // sauter l'en-tête
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.replaceAll("^\"|\"$", "").split("\",\"");
                if (parts.length >= 2) {
                    textes.add(parts[0].trim());
                    labels.add(parts[1].trim());
                }
            }
        }

        // Attributs Weka
        attributes = new ArrayList<>();
        attributes.add(new Attribute("texte", (ArrayList<String>) null));
        ArrayList<String> classValues = new ArrayList<>();
        for (String label : labels) if (!classValues.contains(label)) classValues.add(label);
        attributes.add(new Attribute("classe", classValues));

        Instances dataset = new Instances("Domaines", attributes, textes.size());
        dataset.setClassIndex(1);

        for (int i = 0; i < textes.size(); i++) {
            DenseInstance inst = new DenseInstance(2);
            inst.setValue(attributes.get(0), textes.get(i));
            inst.setValue(attributes.get(1), labels.get(i));
            dataset.add(inst);
        }

        // TF-IDF
        filter = new StringToWordVector();
        filter.setTFTransform(true);
        filter.setIDFTransform(true);
        filter.setLowerCaseTokens(true);
        filter.setStopwordsHandler(new Rainbow());
        filter.setInputFormat(dataset);
        filteredData = Filter.useFilter(dataset, filter);

        // Entraînement NaiveBayes
        model = new NaiveBayes();
        model.buildClassifier(filteredData);
    }

    // Méthode prédiction d'une seule phrase
    public static String predict(String phrase) throws Exception {
        DenseInstance newInst = new DenseInstance(2);
        newInst.setDataset(filteredData);
        newInst.setValue(attributes.get(0), phrase.trim());

        Instances newDataset = new Instances(filteredData);
        newDataset.clear();
        newDataset.add(newInst);

        Instances filteredNew = Filter.useFilter(newDataset, filter);
        double predIndex = model.classifyInstance(filteredNew.firstInstance());
        return filteredData.classAttribute().value((int) predIndex);
    }
}