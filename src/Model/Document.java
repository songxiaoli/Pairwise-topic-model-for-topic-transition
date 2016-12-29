package Model;

import java.util.HashMap;

public class Document {
	HashMap<Integer, Integer> topics = new HashMap<Integer,Integer>();
    HashMap<Integer,Integer> words = new HashMap<Integer,Integer>(); 
    HashMap<Integer,Integer> tf = new HashMap<Integer,Integer>();
    public Document(String[] words){
    	int count = 0;
    	for(int i = 0; i < words.length; i++){
    		if(words[i].length() == 0)continue;
            this.words.put(count++,Integer.parseInt(words[i]));
    	}
    }
    
    public void setTopic(int wordId, int topicId){
    	if(topics.containsKey(wordId))
    	    topics.remove(wordId);
    	topics.put(wordId, topicId);
    }
    public int getDocLen(){
    	return this.words.size();
    }   
}