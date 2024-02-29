package org.example.controller;

import org.example.common.Pair;
import org.example.model.Model;
import org.example.view.View;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.CorefCoreAnnotations;
import edu.stanford.nlp.trees.*;

public class Controller {

    private static Model model;
    private static View view;

    public Controller(Model model, View view) {
        Controller.model = model;
        Controller.view = view;
        attachListeners();
    }

    //method that contains all Listeners
    public void attachListeners() {
        //Listener for choosing a file
        view.getFileChooserButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int returnValue = fileChooser.showOpenDialog(null);

                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    model.setFile(fileChooser.getSelectedFile());
                    view.getFileNameLabel().setText(fileChooser.getSelectedFile().getName());
                }
            }
        });
        //Listener for GenerateRdfXml Button
        view.getGenerateRdfButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (model.getFile() != null) {
                    try{
                        model.setRelations(new LinkedHashMap<>());

                        String paragraph = readFile(model.getFile()); //Read the chosen file

                        String relations = performNLP(paragraph); //Generate triples (subject, verb, object)

                        String rdfXml = transformToRDF(relations); //Transform the triples into rdf/xml format

                        view.getRdfTextArea().setText("");
                        view.getRdfTextArea().setText(rdfXml); //update the text are with the rdf/xml data generated before

                    }catch(IOException ex)
                    {
                        Logger logger = Logger.getLogger(Controller.class.getName());
                        logger.log(Level.SEVERE, "An error occurred", e);
                    }

                }
            }
        });
        //Listener for GenerateGraph Button
        view.getGenerateGraphButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateGraph();
            }
        });
        //Listener for Clear Textarea Button
        view.getClearButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setRelations(new LinkedHashMap<>());
                view.getRdfTextArea().setText("");
            }
        });
    }

    private void generateGraph()
    {
        String rdf = view.getRdfTextArea().getText(); //Get the text from the textarea
        String graphImageUrl = sendRDFAndGetGraphImage(rdf); //Retrieve the image url from W3C-rdf-validator
        displayGraphImage(graphImageUrl); //Display the image
    }

    private String sendRDFAndGetGraphImage(String rdf)
    {
        String image = "";

        String url = "https://www.w3.org/RDF/Validator/"; //the web page that we want to perform web scraping on

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        WebDriver driver = new ChromeDriver(options); //initialize the web driver
        driver.get(url); //open the web page

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5)); //define wait element of 5 seconds to give time to the page to load

        WebElement textarea = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("RDF"))); //find the textarea in the webpage
        textarea.clear(); //clear the textarea in case it already has text
        textarea.sendKeys(rdf); //send the text that the user entered to the textarea in the webpage

        Select dropdown = new Select(driver.findElement(By.name("TRIPLES_AND_GRAPH"))); //find the dropdown list on the webpage
        dropdown.selectByIndex(1); //select the second option which let us generate a rdf graph and triples
        WebElement button = driver.findElement(By.cssSelector("input[value='Parse RDF']")); //find the button on the webpage used to parse the rdf
        button.click(); //click the button to parse rdf

        String script = "return document.body.textContent"; //here we want to check if there is any error,the error in webpage is not included in tags, so we have to retrieve the whole textContent
        String text = (String) ((JavascriptExecutor) driver).executeScript(script); //we use javascript to execute the script

        //check if the return text contains error message or fatal error
        if(text.contains("Error MessagesError") || text.contains("FatalError"))
        {
            int startIndex = text.indexOf("Error MessagesError"); //get the index of MessageError

            if(startIndex != -1) //if index exist
            { int endIndex = text.indexOf("\n", startIndex); //get the index of "\n" ie the end of the error message
                String extractedText = text.substring(startIndex, endIndex); //extract the text from Error MessageError to the end line
                JOptionPane.showMessageDialog(null, extractedText, "Error", JOptionPane.ERROR_MESSAGE); //show an error message
            }

            startIndex = text.indexOf("FatalError"); //get the index of FatalError
            if(startIndex != -1) //if index exist
            {
                int endIndex = text.indexOf("\n", startIndex); //get the index of "\n" ie the end of the fatal error message
                String extractedText = text.substring(startIndex, endIndex); //extract the text from FatalError to the end of line
                JOptionPane.showMessageDialog(null, extractedText, "Error", JOptionPane.ERROR_MESSAGE); //show an error message
            }
        }
        else{ //otherwise if there are no error, so the rdf is parsed
            WebElement img = driver.findElement(By.cssSelector("img[alt='graph representation of RDF data']")); //retrieve the img tag
            image = img.getAttribute("src"); //get the source ie the link of the image

        }

        return image; //return the image link
    }

    private void displayGraphImage(String imageUrl) {
        ImageIcon imageIcon = null;
        int maxWidth = 1000;
        int maxHeight = 700;
        //uncomment the lines below to rescale the image
        try{
            if(!imageUrl.isEmpty())
            {
                imageIcon = new ImageIcon(new URL(imageUrl)); //create an image icon of the image retrieved from W3C-rdf-validator after we validated our rdf/xml
                int imgWidth = imageIcon.getIconWidth();
                int imgHeight = imageIcon.getIconHeight();
                double widthScale = (double) maxWidth / imgWidth; //scale the retrieved image's width
                double heightScale = (double) maxHeight / imgHeight; //scale the retrieved image's height
                double scale = Math.min(widthScale, heightScale);
                int newWidth = (int) (imgWidth * scale);
                int newHeight = (int) (imgHeight * scale);

                Image scaledImage = imageIcon.getImage().getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH); //generate a new scaled instance of the retrieved image
                imageIcon = new ImageIcon(scaledImage);
                view.getGraphImageLabel().setIcon(imageIcon); //set the image label, to the generated image
            }
        }
        catch (Exception e) {
            Logger logger = Logger.getLogger(Controller.class.getName());
            logger.log(Level.SEVERE, "An error occurred", e);
        }

    }

    //Method to read the file chosen by the user
    private static String readFile(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        try(BufferedReader br = new BufferedReader(new FileReader(file)))
        {
            String line;
            while((line = br.readLine()) != null)
            {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    //Method to generate triples (subject, verb, object) statements from the paragraph (generated from the text file chosen by the user)
    private static String performNLP(String paragraph)
    {
        String[] sentences = performSentenceDetection(paragraph); //Detect sentences in the paragraph

        LinkedHashMap<String, ArrayList<Pair<String,String>>> relations = model.getRelations(); // subject-verb-object relations stored as Hashmap with 'key=subject' and 'value=Pair(verb, object)'

        Properties props = new Properties(); //define properties
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, coref"); //set the required annotators
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props); //define a pipeline with the properties

        //This method will retrieve detected coreferences in the paragraph
        //Example "Messi plays for miami club. He is Argentinian. He used to play for PSG"
        //Will retrieve a hashmap with 'key=Messi' and 'value=[He,He]'
        //This will help us later replace 'He' with its representative word 'Messi'
        HashMap<String, ArrayList<String>> coreference = getCoreferences(paragraph);

        assert sentences != null;
        for(String sentence : sentences) //After splitting the paragraph into sentences, we will generate triples from each sentence
        {
            //Create an Annotation just with the given sentence
            Annotation document = new Annotation(sentence);
            //Run all Annotators on this text
            pipeline.annotate(document);
            //get the tree structure of the sentence
            CoreMap sent = document.get(CoreAnnotations.SentencesAnnotation.class).get(0);
            Tree tree = sent.get(TreeCoreAnnotations.TreeAnnotation.class);
            //define empty subject-verb-object
            String subject = "";
            String verb = "";
            String object = "";
            //traverse the tree
            for(Tree subtree : tree)
            {   //check for nouns
                if(subtree.label().value().equals("NNP") || subtree.label().value().equals("NNPS") || subtree.label().value().equals("NN") || subtree.label().value().equals("CD") || subtree.label().value().equals("NNS") || subtree.label().value().equals("JJ") || subtree.label().value().equals("DT"))
                {
                    if(verb.isEmpty()) subject += subtree.getLeaves().toString(); //if we don't have verb yet then the noun is subject
                    else object += subtree.getLeaves().toString(); //otherwise the noun is object

                }
                //check for verbs
                else if(subtree.label().value().equals("VB") || subtree.label().value().equals("VBZ") || subtree.label().value().equals("VBD") || subtree.label().value().equals("VBG") || subtree.label().value().equals("VBN") || subtree.label().value().equals("VBP"))
                {
                    verb += subtree.getLeaves().toString();
                }
                else if(subtree.label().value().equals("IN")) //check for prepositions
                {
                    if(verb.isEmpty()) subject += subtree.getLeaves().toString(); //if no verb yet then concatenate it with subject
                    else if(object.isEmpty()) verb += subtree.getLeaves().toString(); //if verb exist and object don't concatenate it with verb
                    else object += subtree.getLeaves().toString(); //otherwise concatenate it with object
                }
                else if(subtree.label().value().equals("CC")) //check for coordinating conjunctions
                {
                    if(verb.isEmpty()) subject += "+"; //if verb empty then it refers to subject we add + to subject to separate two subjects
                    else if(object.isEmpty()) verb += "+";// if verb exist and object don't we add + to verb to separate two verbs
                    else object += "+"; //otherwise add + to object to separate two objects
                }
                else if(subtree.label().value().equals("PRP") || subtree.label().value().equals("PRP$"))//check for personal or possessive pronouns
                {
                    String prp = subtree.getLeaves().toString(); //get the pronoun
                    prp = prp.replaceAll("[\\[\\]]","");
                    //check for the pronoun in the coreference list to get the representative word
                    for (Map.Entry<String, ArrayList<String>> entry : coreference.entrySet()) {
                        String key = entry.getKey();
                        ArrayList<String> values = entry.getValue();

                        // Check if the target string exists in the ArrayList
                        if (values.contains(prp)) {
                            // If found, remove the element
                            values.remove(prp);
                            prp = key; //set the prp to the representative word
                            break;
                        }
                    }
                    subject = prp; //set the subject to the representative word
                }

            }
            relations = saveSVO(relations, subject, verb, object);
        }

        return createRelation(relations); //return the created relation from s-v-o
    }
    //Method to detect sentences in the paragraph
    private static String[] performSentenceDetection(String paragraph)
    {
        try(InputStream modelIn = new FileInputStream("src/main/java/org/example/asset/en-sent.bin"))
        {
            SentenceModel model = new SentenceModel(modelIn);
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
            return sentenceDetector.sentDetect(paragraph);

        }catch(IOException e){
            Logger logger = Logger.getLogger(Controller.class.getName());
            logger.log(Level.SEVERE, "An error occurred", e);
            return null;
        }
    }
    //This method takes a relation s-v-o and build and rdf/xml

    private static String createRelation(LinkedHashMap<String,ArrayList<Pair<String,String>>> relations)
    {
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,ArrayList<Pair<String,String>>> entry : relations.entrySet())
        {
            String key = entry.getKey();
            ArrayList<Pair<String,String>> pairs = entry.getValue();

            sb.append(" <rdf:Description rdf:about=\"http://example.org/").append(key).append("\">\n");
            for(Pair<String,String> pair : pairs)
            {
                sb.append("   <mydomain:").append(pair.getKey()).append(" rdf:resource=\"http://example.org/").append(pair.getValue()).append("\"/>\n");
            }
            sb.append(" </rdf:Description>\n");
        }

        return sb.toString();
    }
    //This method builds the whole rdf/xml for all relations
    private static String transformToRDF(String relations)
    {
        StringBuilder rdfBuilder = new StringBuilder();
        rdfBuilder.append("<?xml version=\"1.0\"?>\n");
        rdfBuilder.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
        rdfBuilder.append("         xmlns:mydomain=\"http://www.mydomain.org/my-rdf-ns#\">\n");
        rdfBuilder.append(relations);
        rdfBuilder.append("</rdf:RDF>");

        return rdfBuilder.toString();
    }
    //This method return references in a paragraph
    //Example: The Lebanese University was founded in 1951. It is the top Lebanese University. Its president is Professor Bassam Badran.
    //It will return Hashmap with key 'The Lebanese University' and values , the possible pronouns that reference it in this case [It, Its]
    private static HashMap<String, ArrayList<String>> getCoreferences(String paragraph) {

        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner,parse, coref");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        Annotation doc = new Annotation(paragraph);
        pipeline.annotate(doc);

        //Get the corefchain
        Map<Integer, CorefChain> corefChains = doc.get(CorefCoreAnnotations.CorefChainAnnotation.class);
        //define a hashmap to save pronouns and its representative word
        HashMap<String, ArrayList<String>> correlations = new HashMap<>();
        //iterate over the corefchain
        for (Map.Entry<Integer, CorefChain> entry : corefChains.entrySet()) {
            int chainID = entry.getKey();
            CorefChain corefChain = entry.getValue(); //get the value

            int i=0;
            String key = "";
            // Get all mentions in textual order
            for (CorefChain.CorefMention mention : corefChain.getMentionsInTextualOrder()) {

                if(i==0)//the first mention is the representative word, we only need it once
                {
                    key = mention.mentionSpan;
                    correlations.put(key, new ArrayList<>());
                    i++;
                }
                else{//the rest mentions are the pronouns
                    correlations.get(key).add(mention.mentionSpan);
                }
            }
        }

        return correlations;

    }

    private static LinkedHashMap<String, ArrayList<Pair<String, String>>> saveSVO(LinkedHashMap<String, ArrayList<Pair<String, String>>> relations, String subject, String verb, String object)
    {
        //we remove all brackets and whitespaces from the s-v-o
        subject = subject.replaceAll("[\\[\\]\\s]", "").toLowerCase();
        verb = verb.replaceAll("[\\[\\]\\s]", "").toLowerCase();
        object = object.replaceAll("[\\[\\]\\s]", "").toLowerCase();
        //this is to check if we have encountered a coordinating conjunction that requires special treatment
        if(subject.contains("+") || verb.contains("+") || object.contains("+"))
        {   //if we have 2 subjects 2 verbs and 2 objects in the sentence
            if(subject.contains("+") && verb.contains("+") && object.contains("+"))
            {
                String[] subjects = subject.split("\\+"); //get all subjects
                String[] verbs = verb.split("\\+");// get all verbs
                String[] objects = object.split("\\+"); //get all objects

                for(String sub : subjects) //each subject has a certain verb and object, so we must loop all of them
                {
                    for(String ver : verbs)
                    {
                        for(String obj : objects)
                        {
                            if(relations.containsKey(sub))//if subject already added to relations
                            {
                                relations.get(sub).add(new Pair<>(ver,obj));//we assign new verb-object to the subject
                            }
                            else
                            {   //otherwise we create verb-object and insert the subject with its values for the first time
                                ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                                verbObject.add(new Pair<>(ver,obj));
                                relations.put(sub, verbObject);
                            }
                        }

                    }
                }
            }
            else if(subject.contains("+") && verb.contains("+") && !object.contains("+")) //we have 2 subjects, 2 verbs and 1 object
            {
                String[] subjects = subject.split("\\+");
                String[] verbs = verb.split("\\+");

                for(String sub : subjects)
                {
                    for(String ver : verbs)
                    {

                        if(relations.containsKey(sub))
                        {
                            relations.get(sub).add(new Pair<>(ver,object));
                        }
                        else
                        {
                            ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                            verbObject.add(new Pair<>(ver,object));
                            relations.put(sub, verbObject);
                        }
                    }

                }
            }
            else if(subject.contains("+") && !verb.contains("+") && object.contains("+"))//we have 2 subjects, 1 verb and 2 objects
            {
                String[] subjects = subject.split("\\+");
                String[] objects = object.split("\\+");

                for(String sub : subjects)
                {
                    for(String obj : objects)
                    {

                        if(relations.containsKey(sub))
                        {
                            relations.get(sub).add(new Pair<>(verb,obj));
                        }
                        else
                        {
                            ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                            verbObject.add(new Pair<>(verb,obj));
                            relations.put(sub, verbObject);
                        }
                    }

                }
            }
            else if(!subject.contains("+") && verb.contains("+") && object.contains("+"))//we have 1 subject, 2 verbs and 2 objects
            {
                String[] verbs = verb.split("\\+");
                String[] objects = object.split("\\+");

                for(String ver : verbs)
                {
                    for(String obj : objects)
                    {

                        if(relations.containsKey(subject))
                        {
                            relations.get(subject).add(new Pair<>(ver,obj));
                        }
                        else
                        {
                            ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                            verbObject.add(new Pair<>(ver,obj));
                            relations.put(subject, verbObject);
                        }
                    }

                }
            }
            else if(subject.contains("+") && !verb.contains("+") && !object.contains("+"))//we have 2 subjects, 1 verb and 1 object
            {
                String[] subjects = subject.split("\\+");

                for(String sub : subjects)
                {

                    if(relations.containsKey(sub))
                    {
                        relations.get(subject).add(new Pair<>(verb,object));
                    }
                    else
                    {
                        ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                        verbObject.add(new Pair<>(verb,object));
                        relations.put(sub, verbObject);
                    }


                }
            }
            else if(!subject.contains("+") && verb.contains("+") && !object.contains("+"))//we have 1 subject, 2 verbs and 1 object
            {
                String[] verbs = verb.split("\\+");

                for(String ver : verbs)
                {

                    if(relations.containsKey(subject))
                    {
                        relations.get(subject).add(new Pair<>(ver,object));
                    }
                    else
                    {
                        ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                        verbObject.add(new Pair<>(ver,object));
                        relations.put(subject, verbObject);
                    }


                }
            }
            else if(!subject.contains("+") && !verb.contains("+") && object.contains("+"))//we have 1 subject, 1 verb and 2 objects
            {
                String[] objects = object.split("\\+");

                for(String obj : objects)
                {

                    if(relations.containsKey(subject))
                    {
                        relations.get(subject).add(new Pair<>(verb,obj));
                    }
                    else
                    {
                        ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                        verbObject.add(new Pair<>(verb,obj));
                        relations.put(subject, verbObject);
                    }


                }
            }
        }
        else{
            if(relations.containsKey(subject))//if subject already added to relations
            {
                relations.get(subject).add(new Pair<>(verb,object));//we assign new verb-object to the subject
            }
            else
            {   //otherwise we create verb-object and insert the subject with its values for the first time
                ArrayList<Pair<String,String>> verbObject = new ArrayList<>();
                verbObject.add(new Pair<>(verb,object));
                relations.put(subject, verbObject);
            }
        }

        return relations;
    }
}
