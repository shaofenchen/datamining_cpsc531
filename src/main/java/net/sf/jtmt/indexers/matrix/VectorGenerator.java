package net.sf.jtmt.indexers.matrix;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jtmt.recognizers.BoundaryRecognizer;
import net.sf.jtmt.recognizers.ContentWordRecognizer;
import net.sf.jtmt.recognizers.IRecognizer;
import net.sf.jtmt.recognizers.RecognizerChain;
import net.sf.jtmt.recognizers.StopwordRecognizer;
import net.sf.jtmt.tokenizers.Token;
import net.sf.jtmt.tokenizers.TokenType;
import net.sf.jtmt.tokenizers.WordTokenizer;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.linear.OpenMapRealMatrix;
import org.apache.commons.math.linear.RealMatrix;

import cpsc531.tc.stemmer.IStemmer;

/**
 * Generate the word occurence vector for a document collection.
 * 
 * @author Sujit Pal
 * @version $Revision: 21 $
 */
public class VectorGenerator {

	//private DataSource dataSource;

	//private Map<Integer, String> wordIdValueMap = new TreeMap<Integer, String>();
	//private Map<Integer, String> documentIdNameMap = new TreeMap<Integer, String>();
	private ArrayList<String> wordList = new ArrayList<String>();
	private ArrayList<String> documentNameList = new ArrayList<String>();
	private RealMatrix matrix;
	
	private WordTokenizer wordTokenizer;
	private IStemmer stemmer;
	private RecognizerChain recognizerChain;

	public void generateVector(Map<String, Reader> documents) throws Exception {

		initRecognizers();
		
		Map<String, Bag<String>> documentWordFrequencyMap = new HashMap<String, Bag<String>>();
		SortedSet<String> wordSet = new TreeSet<String>();
		Integer docId = 0;
		for (String key : documents.keySet()) {
			String text = getText(documents.get(key));
			Bag<String> wordFrequencies = getWordFrequencies(text);
			wordSet.addAll(wordFrequencies.uniqueSet());
			documentWordFrequencyMap.put(key, wordFrequencies);
			documentNameList.add(docId, key);
			docId++;
		}
		// create a Map of ids to words from the wordSet
		int wordId = 0;
		for (String word : wordSet) {
			wordList.add(wordId, word);
			wordId++;
		}

//		System.out.println("********generate fre********");
//		estimatedTime = System.nanoTime() - startTime;
//		startTime = System.nanoTime();
//		System.out.println(estimatedTime);

		int numDocs = documents.keySet().size();
		int numWords = wordSet.size();
		matrix = new OpenMapRealMatrix(numWords, numDocs);
		for (int i = 0; i < matrix.getRowDimension(); i++) {
			for (int j = 0; j < matrix.getColumnDimension(); j++) {
				String docName = documentNameList.get(j);
				Bag<String> wordFrequencies = documentWordFrequencyMap
						.get(docName);
				String word = wordList.get(i);
				int count = wordFrequencies.getCount(word);
				matrix.setEntry(i, j, count);
			}
		}
		releaseRecognizers();
		
//		System.out.println("********generate matrix********");
//		estimatedTime = System.nanoTime() - startTime;
//		System.out.println(estimatedTime);
	}

	private void initRecognizers() throws Exception {
		wordTokenizer = new WordTokenizer();
		recognizerChain = new RecognizerChain(
				Arrays.asList(new IRecognizer[] { new BoundaryRecognizer(),
						// new AbbreviationRecognizer(dataSource),
						// new PhraseRecognizer(dataSource),
						new StopwordRecognizer(), new ContentWordRecognizer(stemmer) }));
		recognizerChain.init();
	}
	
	private void releaseRecognizers() throws Exception {
		if(wordTokenizer != null) wordTokenizer = null;
		if(recognizerChain != null) recognizerChain = null;
	}

	public RealMatrix getMatrix() {
		return matrix;
	}

	public String[] getDocumentNames() {
		String[] documentNames = new String[documentNameList.size()];
		return (String[]) documentNameList.toArray(documentNames);
//		String[] documentNames = new String[documentIdNameMap.keySet().size()];
//		for (int i = 0; i < documentNames.length; i++) {
//			documentNames[i] = documentIdNameMap.get(i);
//		}
//		return documentNames;
	}
	public ArrayList<String> getDocumentNameList(){
		return documentNameList;
	}

	public String[] getWords() {
		String[] words = new String[wordList.size()];
		words = (String[]) wordList.toArray(words);
		for (int i = 0; i < words.length; i++) {
			//String word = wordIdValueMap.get(i);
			String word = words[i];
			if (word.contains("|||")) {
				// phrases are stored with length for other purposes, strip it
				// off for this report.
				word = word.substring(0, word.indexOf("|||"));
				words[i] = word;
			}
		}
		return words;
	}
	
	public void addWord(String word){
		wordList.add(word);
	}
	
	public void addDocumentName(String docName){
		documentNameList.add(docName);
	}
	
	public void setStemmer(IStemmer _stemmer){
		stemmer = _stemmer;
		
	}
	
	public ArrayList<String> getWordList(){
		return wordList;
	}
	
	public String getDocumentName(int index){
		return documentNameList.get(index);
	}
	
	public String getWord(int index){
		return wordList.get(index);
	}
	
	private Bag<String> getWordFrequencies(String text) throws Exception {
		Bag<String> wordBag = new HashBag<String>();
		// WordTokenizer wordTokenizer = new WordTokenizer();
		wordTokenizer.setText(text);
		List<Token> tokens = new ArrayList<Token>();
		Token token = null;
		while ((token = wordTokenizer.nextToken()) != null) {
			tokens.add(token);
		}
//		RecognizerChain recognizerChain = new RecognizerChain(
//				Arrays.asList(new IRecognizer[] { new BoundaryRecognizer(),
//						// new AbbreviationRecognizer(dataSource),
//						// new PhraseRecognizer(dataSource),
//						new StopwordRecognizer(), new ContentWordRecognizer() }));
//		recognizerChain.init();
		List<Token> recognizedTokens = recognizerChain.recognize(tokens);
		for (Token recognizedToken : recognizedTokens) {
			if (// recognizedToken.getType() == TokenType.ABBREVIATION ||
				// recognizedToken.getType() == TokenType.PHRASE ||
			recognizedToken.getType() == TokenType.CONTENT_WORD) {
				// lowercase words to treat Human and human as the same word
				wordBag.add(StringUtils.lowerCase(recognizedToken.getValue()));
			}
		}
		return wordBag;
	}

	private String getText(Reader reader) throws Exception {
		StringBuilder textBuilder = new StringBuilder();
		char[] cbuf = new char[1024];
		int len = 0;
		while ((len = reader.read(cbuf, 0, 1024)) != -1) {
			textBuilder.append(ArrayUtils.subarray(cbuf, 0, len));
		}
		reader.close();
		return textBuilder.toString();
	}
}
