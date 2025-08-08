import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * This class is created for PS-5 in COSC 10, and is designed to, given a complete sentence,
 * print out that sentence with each word's part of speech labeled.
 * It uses a Hidden Markov model and a Viterbi search to calculate the most probable configuration of the words in the
 * sentence. There are a few built in tests from the assignment that can be performed when the main file is run, 
 * after which the system takes strings to label the parts of speech of. 
 * Everything was done in one file.
 */
public class PartOfSpeechLabeler {

    //Maps each part of speech to all other parts of speech it can transition to and the odds of that transition
    private HashMap<String, HashMap<String, Double>> transitionProbMap;

    //Maps each part of speech to every word in that part of speech and the odds that that word was used
    private HashMap<String, HashMap<String, Double>> observePartWordMap;

    //Briefly thought about trying to keep everything in one map, but decided that it was not worth it because
    //punctuation would mess it up

    private double unseenWordPenalty = -100.0; // play around with this to fine tune the operations
    //After trying some of them, the errors stop being reduced around -15, so there may be some sweetspot
    //around -15 where there might be slightly less errors
    //I found the right integer, at -16, may be even more precise. It gets around 7 more words right than -100,
    //with three more sentences completely right.

    /**
     * Constructor to read from 2 files and get the data necessary for the searches.
     * Trains the maps in the way outlined in the assignment description.
     * @param trainTagsFile file to get the tags from, with each one corresponding to a word in:
     * @param trainSentencesFile file with each line being a sentence corresponding to the tags file.
     */
    public PartOfSpeechLabeler(String trainTagsFile, String trainSentencesFile){
        //Initialize instance variables
        transitionProbMap = new HashMap<>();
        observePartWordMap = new HashMap<>();

        //variables to be used while creating the instance variables
        HashMap<String, HashMap<String, Integer>> transitionsMap = new HashMap<>();
        HashMap<String, HashMap<String, Integer>> partWordMap = new HashMap<>();

        //BufferedReaders to read from the training files
        BufferedReader tagReader;
        BufferedReader wordReader;

        try {
            tagReader = new BufferedReader(new FileReader(trainTagsFile));
            wordReader = new BufferedReader(new FileReader(trainSentencesFile));
        }
        catch (Exception e){
            System.out.println(e);
            System.out.println("File probably was not found");
            return;
        }

        String tagLine = "Will store the string of tags";
        String wordLine = "Will store the sequence of words";
        while(tagLine!=null || wordLine != null){
            try{
                tagLine = tagReader.readLine();
                wordLine = wordReader.readLine();

                if(tagLine!=null && wordLine!=null){

                    wordLine = wordLine.toLowerCase(); //does this change anything?
                    String[] tagArray = tagLine.split(" ");
                    String[] wordArray = wordLine.split(" ");
                    String prevTag = "#";//The starting previous word

                    if(tagArray.length == wordArray.length){//check that each word corresponds to a part of speech
                        for(int i = 0; i<tagArray.length; i++){
                            if(!transitionsMap.containsKey(prevTag)){transitionsMap.put(prevTag,new HashMap<>());}
                            if(!transitionsMap.get(prevTag).containsKey(tagArray[i])){
                                transitionsMap.get(prevTag).put(tagArray[i], 0);
                            }

                            //Increments the value of that transition in parts of speech by one
                            transitionsMap.get(prevTag).put(tagArray[i],transitionsMap.get(prevTag).get(tagArray[i])+1);

                            if(!partWordMap.containsKey(tagArray[i])){partWordMap.put(tagArray[i],new HashMap<>());}
                            if(!partWordMap.get(tagArray[i]).containsKey(wordArray[i])){
                                partWordMap.get(tagArray[i]).put(wordArray[i], 0);
                            }

                            //Increments the number of usages of the word as that part of speech by one
                            partWordMap.get(tagArray[i]).put(wordArray[i],partWordMap.get(tagArray[i]).get(wordArray[i])+1);

                            //move previous tag to the current for the next looping
                            prevTag = tagArray[i];
                        }

                    }
                }

            }
            catch (Exception e){
                System.out.println(e);
                tagLine = null; //ends loop if something goes wrong
            }
        }//After this loop, partWordMap and transitionMap each store all of the values that they are supposed to

        try{
            tagReader.close();
            wordReader.close();
        }
        catch (Exception e){
            System.out.println(e);//honestly, if it got this far it's probably fine to not return
        }


        //Now, with these variables, initialize the instance variables

        try {
            for (String part1 : transitionsMap.keySet()) {
                transitionProbMap.put(part1, new HashMap<>());
                long total = 0;
                //Get the total number of transitions from that state - need to be long?
                for (String summing : transitionsMap.get(part1).keySet()) {
                    total += transitionsMap.get(part1).get(summing);
                }

                for (String part2 : transitionsMap.get(part1).keySet()) {


                    //log of the number of occurances over the total number of transitions - total above
                    Double logProb = Math.log((double) transitionsMap.get(part1).get(part2) / total);

                    //for testing
                    // System.out.println(part1+" "+ part2+ " " +logProb + " "+(double)transitionsMap.get(part1).get(part2)/total);


                    transitionProbMap.get(part1).put(part2, logProb);
                }
            }//After this loop, transitionProbMap should be initialized with each starting state having a hashmap value
            // to the next state and the logarithmic probability of going there

            for (String part : partWordMap.keySet()) {
                observePartWordMap.put(part, new HashMap<>());
                //get the total number of usages of that part of speech - need to be a long for big data?
                long total = 0;
                for (String word : partWordMap.get(part).keySet()) {
                    total += partWordMap.get(part).get(word);
                }

                for (String word : partWordMap.get(part).keySet()) {
                    Double logProb = Math.log((double) partWordMap.get(part).get(word) / total);

                    //for testing
                    // System.out.println(part+" "+ word+ " " +logProb + " "+(double)partWordMap.get(part).get(word)/total);

                    observePartWordMap.get(part).put(word, logProb);
                }
            }
        }
        catch (Exception e){
            System.out.println(e);

        }


    }//end constructor

    /**
     * Simple hard coded test case to test the graph from the programming drill, and some others that I made up.
     * The test case that is created depends on the number called:
     * 0 for the PD test case
     * 1 for creating a map that labels any sentence with question marks after everything
     */
    public PartOfSpeechLabeler(int pick){

        transitionProbMap = new HashMap<>();
        observePartWordMap = new HashMap<>();

        if(pick == 0) {
            transitionProbMap.put("#", new HashMap<>());
            transitionProbMap.put("NP", new HashMap<>());
            transitionProbMap.put("N", new HashMap<>());
            transitionProbMap.put("CNJ", new HashMap<>());
            transitionProbMap.put("V", new HashMap<>());

            observePartWordMap.put("#", new HashMap<>());
            observePartWordMap.put("NP", new HashMap<>());
            observePartWordMap.put("N", new HashMap<>());
            observePartWordMap.put("CNJ", new HashMap<>());
            observePartWordMap.put("V", new HashMap<>());

            transitionProbMap.get("#").put("NP", 3.0);
            transitionProbMap.get("#").put("N", 7.0);

            transitionProbMap.get("N").put("CNJ", 2.0);
            transitionProbMap.get("N").put("V", 8.0);

            transitionProbMap.get("NP").put("CNJ", 2.0);
            transitionProbMap.get("NP").put("V", 2.0);

            transitionProbMap.get("CNJ").put("NP", 2.0);
            transitionProbMap.get("CNJ").put("V", 4.0);
            transitionProbMap.get("CNJ").put("N", 4.0);

            transitionProbMap.get("V").put("NP", 4.0);
            transitionProbMap.get("V").put("CNJ", 2.0);
            transitionProbMap.get("V").put("N", 4.0);

            observePartWordMap.get("N").put("cat", 4.0);
            observePartWordMap.get("N").put("dog", 4.0);
            observePartWordMap.get("N").put("watch", 2.0);

            observePartWordMap.get("NP").put("chase", 10.0);

            observePartWordMap.get("CNJ").put("and", 10.0);

            observePartWordMap.get("V").put("get", 1.0);
            observePartWordMap.get("V").put("chase", 3.0);
            observePartWordMap.get("V").put("watch", 6.0);

            unseenWordPenalty = -10.0;
        }
        else if(pick ==1){
            transitionProbMap.put("#", new HashMap<>());
            transitionProbMap.get("#").put("?", 1000.0);
            transitionProbMap.put("?",new HashMap<>());
            transitionProbMap.get("?").put("?", 1000.0);

            observePartWordMap.put("?", new HashMap<>());
            observePartWordMap.get("?").put("?", 1000.0);

        }

    }

    /**
     * Getters for the two instance variables, were used for testing at one point.
     * @return returns the transition probability map
     */
    public HashMap<String, HashMap<String, Double>> getTransitionProbMap(){
        return transitionProbMap;
    }
    public HashMap<String, HashMap<String, Double>> getObservePartWordMap(){
        return observePartWordMap;
    }

    /**
     * Helper method to return the observed odds of a word being used as that part of speech when searching.
     * Don't need one for the transition odds because there won't be anything like the unseen word penalty, so it would
     * just be unecessary.
     * @param part part of speech for the check
     * @param word word to look for
     * @return returns the double of the odds that was stored in the instance variable, or unseen word penalty if the
     * combination of part and word were not there.
     */
    public Double observeWord(String part, String word){
        if(!observePartWordMap.containsKey(part)){
            return -1000.0;
            //should never happen, but in case the part is not in
            // this map, return an incredibly small number. Should probably just throw an error.
        }

        //Return the log odds for that word as that part if it is listed as one of them
        if(observePartWordMap.get(part).containsKey(word)){return observePartWordMap.get(part).get(word);}

        //If the word hasn't been seen as this part, return the unseen word penalty
        return unseenWordPenalty;
    }

    /**
     * Method to use the viterbi algorithm in order to find all of the parts of speech for a given sentence.
     * @param sentence the given sentence to find the parts for
     * @return an array of strings that will be "<part of speech for that word>"
     */
    public String[] viterbi(String sentence){
        try {
            String[] words = sentence.split(" ");
            String[] output = new String[words.length];

            /*
              Simple helper class to help with the storage of all of the possible paths. Only relevant to this method,
              so will not be public or put anywhere else.
              Stores the current part of speech, the previous part of speech, and the current score for this possible
              path.
             */
           class VitElement {
                private String current;
                private String prev;
                private Double score;

                public VitElement(String current, String prev, Double score) {
                    this.current = current;
                    this.prev = prev;
                    this.score = score;
                }

                public String getCurrent() {
                    return current;
                }

                public String getPrev() {
                    return prev;
                }

                public Double getScore() {
                    return score;
                }

            }

            /*
            ArrayList will end up with one HashMap for each word.
            This hashmap stores a bunch of predicted parts of speech for that word, as well as a VitElement that stores that
            predicted state alongside the previous in that path and the current score in that path.
             */
            ArrayList<HashMap<String, VitElement>> paths = new ArrayList<>();

            int index = 0;

            for (String word : words) {

                paths.add(new HashMap<>());

                /*
                The first word will be slightly different, as the algorithm has to iterate through the map from the
                start state, with no previous paths stored in the ArrayList to go from.
                Also does not have any previous score to keep track of.

                For every other index, there will be a map of states in the previous index to iterate through,
                from which it calculates the score of each of the next transitions from and only adds them to the map
                for the word if there has been no recorded path for that part of speech yet, or the score of the new
                path exceeds that of the previously found path.
                 */
                if (index == 0) {
                    for (String nxtPart : transitionProbMap.get("#").keySet()) {
                        Double transitionOdds = transitionProbMap.get("#").get(nxtPart);
                        paths.get(0).put(nxtPart, new VitElement(nxtPart, "#", transitionOdds + observeWord(nxtPart, word)));
                    }
                }
                else {
                    for (String prevPart : paths.get(index - 1).keySet()) {
                        if(transitionProbMap.get(prevPart)!=null) {//need to check for dead ends, seemingly only for the
                            //simple file data

                            for (String nxtPart : transitionProbMap.get(prevPart).keySet()) {

                                Double odds = transitionProbMap.get(prevPart).get(nxtPart)
                                        + observeWord(nxtPart, word)
                                        + paths.get(index - 1).get(prevPart).getScore();

                                if (!paths.get(index).containsKey(nxtPart) || paths.get(index).get(nxtPart).getScore() < odds) {
                                    paths.get(index).put(nxtPart, new VitElement(nxtPart, prevPart, odds));
                                }

                            }
                        }
                    }
                }


                index++;
            }

            /*
            Now, this finds the state with the greatest score among those in the final index.
             */
            VitElement greatest = null;

            for (String lastVitElement : paths.get(index - 1).keySet()) {
                if (greatest == null || greatest.getScore() < paths.get(index - 1).get(lastVitElement).getScore()) {
                    greatest = paths.get(index - 1).get(lastVitElement);
                }
            }

            /*
            This goes through the output array backwards, adding each of the parts along the path with the greatest
            final score to the array.
             */
            for (int i = words.length - 1; i >= 0; i--) {
                output[i] = greatest.getCurrent();

                //have to make sure not to check index -1.
                if (i > 0) {
                    greatest = paths.get(i - 1).get(greatest.getPrev());
                }
            }

            return output;
        }
        catch (Exception e){
            System.out.println(e);
            return null;
        }
    }

    /**
     * Testing method, used to compare results to that of the example in the assignment.
     * Code is pretty self explanatory, it just runs the Viterbi algorithm on each of the sentences and compares
     * each of the tags it gets to the given file, incrementing each of the stats as necessary.
     * @param testSensFile file of sentences to perform the test on
     * @param testTagsFile corresponding file of answers to check against
     * @return a string that states all of the results of the test, with the number of words and sentences missed,
     * and the number of mistakes in the files that cause a mismatched number of words.
     */
    public String testCompare(String testSensFile, String testTagsFile){
        BufferedReader sentences;
        BufferedReader tags;

        //Each statistic to keep track of
        int missedSentences = 0;
        int missedWords = 0;
        int totalSentences = 0;
        int totalWords = 0;
        int mismatches = 0;

        try{
            sentences = new BufferedReader(new FileReader(testSensFile));
            tags = new BufferedReader(new FileReader(testTagsFile));

        String sentence = "To take value of sentences";
        String tagLine = "To take value of the tags";
        while(sentence!=null){
            try{
                sentence = sentences.readLine();
                tagLine = tags.readLine();
                totalSentences++;
                int nowMissed = missedWords;

                String[] eachTag = tagLine.split(" ");
                String[] viterbed = viterbi(sentence);
                if(eachTag.length==viterbed.length) {
                    for (int i = 0; i <eachTag.length; i++){
                        totalWords++;
                        if(!eachTag[i].equals(viterbed[i])){missedWords++;}
                    }
                    if(nowMissed!=missedWords){missedSentences++;}
                }
                else {mismatches++;}

            }
            catch(Exception e){
                sentence = null;
            }
        }
        sentences.close();
        tags.close();

        return "The test missed: \n" +
                missedWords +" words out of " +totalWords + " (got "+ (totalWords-missedWords)+ ")" +"\n"+
                "in " +missedSentences +" sentences out of "+ totalSentences+ " (got "+ (totalSentences-missedSentences)+")"+"\n"+
                "with " +mismatches +" mismatched number of tags.";

        }
        catch(Exception e){
            System.out.println(e);
            return null;
        }

    }

    /**
     * Method to get the search with an output that humans can more easily read, based on the method in the
     * assignment for formatting.
     * Also has a rendundant check in case something went wrong and somehow the number of tags got messed up.
     * @param sentence sentence to do the search on
     * @return the sentence with a forward(?) slash and the part of speech after each word
     */
    public String spliceSentence(String sentence){
        try {
            String[] words = sentence.split(" ");
            String[] parts = viterbi(sentence);
            if (words.length != parts.length) {
                return "mismatched sentence";
            }
            String output = "";
            for (int i = 0; i < words.length; i++) {
                output += words[i] + "/" + parts[i] + " ";
            }
            return output;
        }
        catch (Exception e){
            System.out.println(e);
            return "I think this should only happen if you try to input only spaces. Did you really \n " +
                    "think that would work?";
        }
    }

    public void typeInSentences(){
        Scanner input = new Scanner(System.in);
        String sentence = "start";

        System.out.println("Welcome to the Viterbi part of speech labeler!");
        System.out.println("This will use the data from the files given in the constructor to determine the parts of");
        System.out.println("speech for any sentence that you type in.");
        System.out.println("If it used the Brown corpus files for training, please refrain from using uppercase letters," +
                "as the program will convert it to lowercase anyway.");
        System.out.println("Type in 'q' to quit the program.");
        System.out.println("Have fun!");

        while(!sentence.equals("q")){
            sentence = input.nextLine();

            //will this line work?
            sentence = sentence.toLowerCase();

            if(!sentence.equals("q")) {
                System.out.println(spliceSentence(sentence));
            }
        }

        input = null;
        
        
    }


    public static void main(String[] args) {
        String simpleTagsFile = "inputs/PartOfSpeechData/simple-train-tags.txt";
        String simpleSentenceFile = "inputs/PartOfSpeechData/simple-train-sentences.txt";

        String tagsFile = "inputs/PartOfSpeechData/brown-train-tags.txt";
        String sentenceFile = "inputs/PartOfSpeechData/brown-train-sentences.txt";

        String testTagsFile = "inputs/PartOfSpeechData/brown-test-tags.txt";
        String testSentencesFile = "inputs/PartOfSpeechData/brown-test-sentences.txt";

        String simpleTestTagsFile = "inputs/PartOfSpeechData/simple-test-tags.txt";
        String simpleTestSentencesFile = "inputs/PartOfSpeechData/simple-test-sentences.txt";

        PartOfSpeechLabeler labelmaker = new PartOfSpeechLabeler(tagsFile,sentenceFile);



        Scanner input = new Scanner(System.in);
        System.out.println("Would you like to run all of the tests? If yes, input <y>.");
        String yes = input.nextLine();

        if(yes.equals("y")) {
            System.out.println("For the sentence that we did in the programming drill:");
            PartOfSpeechLabeler pdTest = new PartOfSpeechLabeler(0);
            System.out.println(pdTest.spliceSentence("chase watch dog chase watch"));
            System.out.println();
            System.out.println("For the test with the simple data:");
            PartOfSpeechLabeler simpleLabelmaker = new PartOfSpeechLabeler(simpleTagsFile, simpleSentenceFile);
            //for testing, when this one wasn't working
//        System.out.println(simpleLabelmaker.getTransitionProbMap().get("#"));
//        System.out.println(simpleLabelmaker.getObservePartWordMap());
//        System.out.println(simpleLabelmaker.spliceSentence("the dog saw trains in the night ."));
            System.out.println(simpleLabelmaker.testCompare(simpleTestSentencesFile, simpleTestTagsFile));
            System.out.println();
            System.out.println("For the test with the Brown data:");
            System.out.println(labelmaker.testCompare(testSentencesFile, testTagsFile));
            System.out.println();
        }

        input =null;

        labelmaker.typeInSentences();

        
    }
}
