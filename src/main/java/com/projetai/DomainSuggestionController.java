package com.projetai;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
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

public class DomainSuggestionController {

    @FXML
    private TextArea txtInput;

    @FXML
    private TextArea txtResult;

    // Modèle Weka
    private Classifier model;
    private Instances filteredData;
    private StringToWordVector filter;

    @FXML
    private void initialize() {
        try {
            // 1️⃣ Charger le CSV
            ArrayList<String> textes = new ArrayList<>();
            ArrayList<String> labels = new ArrayList<>();

            InputStream is = getClass().getResourceAsStream("/csv/domaines.csv");
            if (is == null) {
                txtResult.setText("CSV introuvable !");
                return;
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            br.readLine(); // sauter l'entête
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.replaceAll("^\"|\"$", "").split("\",\"");
                if (parts.length >= 2) {
                    textes.add(parts[0].trim());
                    labels.add(parts[1].trim());
                }
            }

            // 2️⃣ Attributs Weka
            ArrayList<Attribute> attributes = new ArrayList<>();
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

            // 3️⃣ TF-IDF
            filter = new StringToWordVector();
            filter.setTFTransform(true);
            filter.setIDFTransform(true);
            filter.setLowerCaseTokens(true);
            filter.setStopwordsHandler(new Rainbow());
            filter.setInputFormat(dataset);

            filteredData = Filter.useFilter(dataset, filter);

            // 4️⃣ Entraîner NaiveBayes
            model = new NaiveBayes();
            model.buildClassifier(filteredData);

        } catch (Exception e) {
            e.printStackTrace();
            txtResult.setText("Erreur lors de l'initialisation du modèle !");
        }
    }

    @FXML
    private void onSuggestClick() {
        try {
            String inputText = txtInput.getText().trim();
            if (inputText.isEmpty()) {
                txtResult.setText("Veuillez entrer un texte !");
                return;
            }

            DenseInstance newInst = new DenseInstance(2);
            newInst.setDataset(filteredData);
            newInst.setValue(0, inputText);

            Instances newDataset = new Instances(filteredData);
            newDataset.clear();
            newDataset.add(newInst);

            Instances filteredNew = Filter.useFilter(newDataset, filter);
            double predIndex = model.classifyInstance(filteredNew.firstInstance());
            String predClasse = filteredData.classAttribute().value((int) predIndex);

            txtResult.setText(predClasse);

        } catch (Exception e) {
            e.printStackTrace();
            txtResult.setText("Erreur lors de la prédiction !");
        }
    }


    @FXML
    private ComboBox<String> domaineComboBoxAI;
}