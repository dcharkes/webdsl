package org.webdsl.search;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttributeImpl;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.reader.ReaderProvider;

public class SearchSuggester {

    private static SearchFactory searchfactory;
    private static HashMap<String, AutoCompleter> autoCompleterMap = new HashMap<String, AutoCompleter>();
    private static HashMap<String, SpellChecker> spellCheckMap = new HashMap<String, SpellChecker>();

    static{
        try{
            searchfactory = org.hibernate.search.Search.getFullTextSession(utils.HibernateUtil.getCurrentSession()).getSearchFactory();
        } catch (Exception ex) {
            //ignore exception, because it is probably thrown when running reindex script (using new java vm instance). Hibernate session and thus search factory cannot be retrieved then.
            searchfactory = null;
        }
    }

    public static ArrayList<String> findSpellSuggestions(Class<?> entityClass, String baseDir, List<String> fields,
            int maxSuggestionsPerFieldCount, float accuracy, boolean morePopular, Analyzer analyzer, String toSuggestOn) {

        Map<String, List<String>> fieldSuggestionsMap = new LinkedHashMap<String, List<String>>();

        for (String suggestedField : fields) {
            List<String> fieldSuggestions = findSpellSuggestionsForField(entityClass, baseDir, suggestedField,
                    maxSuggestionsPerFieldCount, accuracy, morePopular, analyzer, toSuggestOn);
            //If toSuggestOn is correctly spelled in one of the fields, don't return suggestions
            if (fieldSuggestions.isEmpty()){
                return new ArrayList<String>();
            }
            fieldSuggestionsMap.put(suggestedField, fieldSuggestions);
        }

        return mergeSuggestions(maxSuggestionsPerFieldCount, fieldSuggestionsMap);
    }

    @SuppressWarnings("deprecation")
    public static ArrayList<String> findSpellSuggestionsForField(Class<?> entityClass, String baseDir,
            String suggestedField, int maxSuggestionCount, float accuracy, boolean morePopular,
            Analyzer analyzer, String toSuggestOn) {

        if (toSuggestOn == null || toSuggestOn.isEmpty())
            return new ArrayList<String>();

        SpellChecker spellChecker = null;
        IndexReader fieldIR = null;
        boolean hasSuggestions = false;

        String indexPath = baseDir+suggestedField;
        try {
            spellChecker = getSpellChecker(indexPath);

            spellChecker.setAccuracy(accuracy);

            TokenStream tokenStream = analyzer.tokenStream(suggestedField, new StringReader(
                    toSuggestOn));
            CharTermAttributeImpl ta = (CharTermAttributeImpl) tokenStream
                    .addAttribute(CharTermAttribute.class);

            ArrayList<String[]> allSuggestions = new ArrayList<String[]>();
            String word;
            String[] suggestions;
            while (tokenStream.incrementToken()) {
                word = ta.term();
                suggestions = null;
                if (!morePopular) {
                    suggestions = spellChecker.suggestSimilar(word, maxSuggestionCount);
                } else {
                    if (fieldIR == null)
                        fieldIR = getIndexReader(entityClass);
                    suggestions = spellChecker.suggestSimilar(word, maxSuggestionCount, fieldIR,
                            suggestedField, true);
                }

                if (suggestions == null || suggestions.length == 0)
                    suggestions = new String[] { word };
                else
                    hasSuggestions = true;

                allSuggestions.add(suggestions);
            }

            if (!hasSuggestions)
                // if no suggestions were found, return empty list
                return new ArrayList<String>();
            else
                return formSuggestions(maxSuggestionCount, allSuggestions);

        } catch (Exception e) {
            org.webdsl.logging.Logger.error("EXCEPTION",e);
            //if something goes wrong, close and remove current SpellChecker instance, so it gets renewed
            try {
                spellChecker.close();
            } catch (IOException e2) {
                org.webdsl.logging.Logger.error("EXCEPTION",e2);
            }
            spellCheckMap.remove(indexPath);
        }
        finally {
            searchfactory.getReaderProvider().closeReader(fieldIR);
        }
        return new ArrayList<String>();
    }

    private static IndexReader getIndexReader(Class<?> entityClass) {
        ReaderProvider readerProvider = searchfactory.getReaderProvider();
        return readerProvider.openReader(searchfactory.getDirectoryProviders(entityClass));
    }

    public static ArrayList<String> findAutoCompletions(Class<?> entityClass, String baseDir, List<String> fields,
            int maxSuggestionsPerFieldCount, Analyzer analyzer, String toSuggestOn) {

        Map<String, List<String>> fieldSuggestionsMap = new LinkedHashMap<String, List<String>>();

        for (String suggestedField : fields) {
            List<String> fieldSuggestions = findAutoCompletionsForField(entityClass, baseDir, suggestedField,
                    maxSuggestionsPerFieldCount, analyzer, toSuggestOn);
            fieldSuggestionsMap.put(suggestedField, fieldSuggestions);
        }

        return mergeSuggestions(maxSuggestionsPerFieldCount, fieldSuggestionsMap);
    }

    @SuppressWarnings("deprecation")
    public static ArrayList<String> findAutoCompletionsForField(Class<?> entityClass, String baseDir,
            String suggestedField, int maxSuggestionCount, Analyzer analyzer, String toSuggestOn) {

        if (toSuggestOn == null || toSuggestOn.isEmpty())
            return new ArrayList<String>();

        AutoCompleter autoCompleter = null;
        String indexPath = baseDir + suggestedField;
        try {
            autoCompleter = getAutoCompleter(indexPath);

            TokenStream tokenStream = analyzer.tokenStream(suggestedField, new StringReader(
                    toSuggestOn));
            CharTermAttributeImpl ta = (CharTermAttributeImpl) tokenStream
                    .addAttribute(CharTermAttribute.class);

            boolean dontstop = tokenStream.incrementToken();
            StringBuilder prefixSb = new StringBuilder( toSuggestOn.length() + 16 );
            String word = "";

            while (dontstop){ //eat up all tokens
                word = ta.term();
                dontstop = tokenStream.incrementToken();
                if(dontstop)
                    prefixSb.append(word ).append( " ");
            }

            String prefix = prefixSb.toString();

            String[] suggestions = autoCompleter.suggestSimilar(word, maxSuggestionCount);


            ArrayList<String> allSuggestions = new ArrayList<String>();

            if (suggestions == null || suggestions.length == 0){
                    return allSuggestions;
            }

            for(int i = 0; i < suggestions.length; i++){
                allSuggestions.add(prefix + suggestions[i]);
            }
            return allSuggestions;

        } catch (Exception e) {
            org.webdsl.logging.Logger.error("EXCEPTION",e);
            //if something goes wrong, close and remove current AutoCompleter instance, so it gets renewed
            try {
                autoCompleter.close();
            } catch (IOException e2) {
                org.webdsl.logging.Logger.error("EXCEPTION",e2);
            }
            autoCompleterMap.remove(indexPath);
        }
        return new ArrayList<String>();
    }

    // fill in suggested words between correct/existing words
    private static ArrayList<String> formSuggestions(int maxSuggestionCount,
            ArrayList<String[]> allSuggestions) {

        ArrayList<String> toReturn = new ArrayList<String>();

        if (allSuggestions.isEmpty())
            return toReturn;

        int maxSuggestions = 1;
        for (String[] strings : allSuggestions)
            maxSuggestions = maxSuggestions * strings.length;
        maxSuggestionCount = Math.min(maxSuggestionCount, maxSuggestions);

        int pos;
        String suggestion;

        for (int i = 0; i < maxSuggestionCount; i++) {
            suggestion = "";
            for (String[] sugArray : allSuggestions) {
                    pos = i % sugArray.length;
                suggestion += sugArray[pos] + " ";
            }
            toReturn.add(suggestion.trim());
        }
        return toReturn;
    }

    private static ArrayList<String> mergeSuggestions(int suggestionNumber,
            Map<String, List<String>> fieldSuggestionsMap) {

        LinkedHashSet<String> suggestionsSet = new LinkedHashSet<String>();

//        for (int suggestionPosition = 0; suggestionPosition <= suggestionNumber; suggestionPosition++) {
//            for (Map.Entry<String, List<String>> fieldSuggestionsEntry : fieldSuggestionsMap
//                    .entrySet()) {
//                List<String> suggestedTerms = fieldSuggestionsEntry.getValue();
//                if (suggestedTerms.size() > suggestionPosition) {
//                    String suggestion = suggestedTerms.get(suggestionPosition);
//                    suggestionsSet.add(suggestion);
//                }
//            }
//        }
//
        for(Map.Entry<String, List<String>> fieldSuggestionsEntry : fieldSuggestionsMap.entrySet()){
            for (String suggestion : fieldSuggestionsEntry.getValue()) {
                suggestionsSet.add(suggestion);
                suggestionNumber--;
                if(suggestionNumber < 1)
                    return new ArrayList<String>(suggestionsSet);
            }
        }
        return new ArrayList<String>(suggestionsSet);
    }

    //Get the reusable AutoCompleter instance for a specific index path, also initializes search factory if not present
    private static synchronized AutoCompleter getAutoCompleter(String indexPath) throws IOException{
        AutoCompleter toReturn = autoCompleterMap.get(indexPath);
        if ( toReturn == null ){
            toReturn = new AutoCompleter(FSDirectory.open(new File(indexPath)));
            //autoCompleterMap.put(indexPath, new AutoCompleter(new RAMDirectory(FSDirectory.open(new File(indexPath)))));
            autoCompleterMap.put(indexPath, toReturn );
        }
        return toReturn;
    }

    private static synchronized SpellChecker getSpellChecker(String indexPath) throws IOException{
        SpellChecker toReturn = spellCheckMap.get(indexPath);
        if ( toReturn == null ){
            toReturn = new SpellChecker(FSDirectory.open(new File(indexPath)));
            spellCheckMap.put(indexPath, toReturn);
        }
        return toReturn;
    }

    public static synchronized void forceSpellCheckerRenewal(String indexPath){
        SpellChecker sp = spellCheckMap.get(indexPath);
        if(sp!=null) {
            try {
                sp.close();
            } catch (IOException e) {
                org.webdsl.logging.Logger.error("EXCEPTION",e);
            }
        }
        spellCheckMap.remove(indexPath);
    }

    public static synchronized void forceAutoCompleterRenewal(String indexPath){
        AutoCompleter ac = autoCompleterMap.get(indexPath);
        if(ac!=null) {
            try {
                ac.close();
            } catch (IOException e) {
                org.webdsl.logging.Logger.error("EXCEPTION",e);
            }
        }
        autoCompleterMap.remove(indexPath);
    }

}