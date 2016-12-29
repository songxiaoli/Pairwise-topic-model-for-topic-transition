package Model;

/**
* The Pairwise Topic Model program implements the pairwise topic model to extract topic transition.
*
* @author  Xiaoli Song
*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PairwiseTopicModel{

	private final int numOfTop;
	private final double alpha;
	private final double beta;
	private final double alphaT;
	private final double betaT;
	private final int numOfTopic;
	private final int numOfIter;

	private Document doc[]; 
	private HashMap<Integer, String> mappingWordId = new HashMap<Integer, String>();

	private final String inputFileWord;
	private final String inputFileId;
	private final String mappingFile;
	private final String outputTheta;
	private final String outputThetaT;
	private final String outputPhi;
	private final String outputPhiT;

	private int dz[][]; // dt[i][j]: the number of entities under topic j in document i;
	private int dzSumZ[]; // dtSumD[j]: the number of entities in topic j over all the documents;
	private int dzz[][][]; // dtt[i][j][k]: the number of entities transform from topic j to topic k in document i;
	private int dzzSumZ[][]; // dttSumTT[i]: the number of entities transformation in document i;
	private int ze[][]; // ze[i][j]: the number of entities j under topic i.
	private int zeSumE[]; // dzSumz[j]: the number of entities j under all the topics.
	private HashMap<String, Integer> zzee = new HashMap<String, Integer>();
	private int zzeeSumZZE[][][];
	
	private final String inputFileWordNew;
	private final String inputFileIdNew;
	private final String mappingFileNew;
	
	private Document docNew[];
	private HashMap<Integer, String> mappingWordIdNew = new HashMap<Integer, String>();

	private int dzNew[][];
	private int dzSumZNew[];
	private int dzzNew[][][];
	private int dzzSumZNew[][];
	private int zeNew[][];
	private int zeSumENew[];
	private HashMap<String, Integer> zzeeNew = new HashMap<String, Integer>();
	private int zzeeSumZZENew[][][];
	

	public PairwiseTopicModel() throws IOException {
		
		FileInputStream fstream_config = new FileInputStream("config.txt");
		DataInputStream instream_config = new DataInputStream(fstream_config);
		BufferedReader br_config = new BufferedReader(new InputStreamReader(instream_config));
		String strLine = "";
		
		HashMap<String, String> paras = new HashMap<String, String>();
		while ((strLine = br_config.readLine()) != null) {
			String para = strLine.split("=")[0].trim();
			String val = strLine.split("=")[1].trim();
			paras.put(para, val);
		}	
		br_config.close();	
			
		this.numOfTop = Integer.parseInt(paras.get("numOfTop"));
		this.numOfTopic = Integer.parseInt(paras.get("numOfTopic"));
		this.alpha = Double.parseDouble(paras.get("alpha"));
		this.beta = Double.parseDouble(paras.get("beta"));
		this.alphaT = Double.parseDouble(paras.get("alphaT"));
		this.betaT = Double.parseDouble(paras.get("betaT"));
		this.inputFileWord = paras.get("inputFileWord");
		this.inputFileId = paras.get("inputFileId");
		this.mappingFile = paras.get("mappingFile");
		this.numOfIter = Integer.parseInt(paras.get("numOfIter"));
		this.outputTheta = paras.get("outputTheta");
		this.outputThetaT = paras.get("outputThetaT");
		this.outputPhi = paras.get("outputPhi");
		this.outputPhiT = paras.get("outputPhiT");
		this.inputFileWordNew = paras.get("inputFileWordNew");
		this.inputFileIdNew = paras.get("inputFileIdNew");
		this.mappingFileNew = paras.get("mappingFileNew");
	}
	
	public int Dictionary() throws IOException {
		int numOfDoc = 0;
		HashSet<String> wordsDict = new HashSet<String>();
		HashMap<String, Integer> mapping = new HashMap<String, Integer>();
		FileInputStream fstream_wordfile1 = new FileInputStream(this.inputFileWord);
		DataInputStream instream_wordfile1 = new DataInputStream(fstream_wordfile1);
		BufferedReader br_wordfile1 = new BufferedReader(new InputStreamReader(instream_wordfile1));
		String strLine = "";

		FileOutputStream outfstream_id = new FileOutputStream(this.inputFileId);
		DataOutputStream out_id = new DataOutputStream(outfstream_id);
		BufferedWriter bw_id = new BufferedWriter(new OutputStreamWriter(out_id));

		FileOutputStream outfstream_mapping = new FileOutputStream(this.mappingFile);
		DataOutputStream out_mapping = new DataOutputStream(outfstream_mapping);
		BufferedWriter bw_mapping = new BufferedWriter(new OutputStreamWriter(out_mapping));
		
		while ((strLine = br_wordfile1.readLine()) != null) {
			numOfDoc ++;
			String wordsPerDoc[] = strLine.split("&");
			for (int i = 0; i < wordsPerDoc.length; i++) {
				if (wordsPerDoc[i].length() != 0)
					wordsDict.add(wordsPerDoc[i].trim().toLowerCase());
			}
		}
		br_wordfile1.close();
		Iterator<String> iter = wordsDict.iterator();
		int count = 0;
		while (iter.hasNext()) {
			String word = iter.next().toString().trim().toLowerCase();
			mapping.put(word, count++);
			bw_mapping.write(word + "\n");
		}
		bw_mapping.close();

		FileInputStream fstream_wordfile2 = new FileInputStream(this.inputFileWord);
		DataInputStream instream_wordfile2 = new DataInputStream(fstream_wordfile2);
		BufferedReader br_wordfile2 = new BufferedReader(new InputStreamReader(instream_wordfile2));
		String strLine2 = "";

		while ((strLine2 = br_wordfile2.readLine()) != null) {
			String wordsPerDoc[] = strLine2.split("&");
			for (int i = 0; i < wordsPerDoc.length; i++) {
				if (wordsPerDoc[i].length() != 0) {
					int id = mapping.get(wordsPerDoc[i].toString().trim()
							.toLowerCase());
					bw_id.write(Integer.toString(id) + " ");
				}
			}
			bw_id.write("\n");
		}
		br_wordfile2.close();
		bw_id.close();
		return numOfDoc;
	}

	public void loadDocument(int numOfDoc) throws IOException {
		this.doc = new Document[numOfDoc];
		FileInputStream fstream = new FileInputStream(this.inputFileId);
		DataInputStream instream = new DataInputStream(fstream);
		BufferedReader brstream = new BufferedReader(new InputStreamReader(instream));
		String strLine = "";
		int countDoc = 0;
		while ((strLine = brstream.readLine()) != null) {
			String wordsPerDoc[] = strLine.split(" ");
			this.doc[countDoc++] = new Document(wordsPerDoc);
		}
		brstream.close();
	}
	
	public void mapping() throws IOException {
		FileInputStream fstream = new FileInputStream(this.mappingFile);
		DataInputStream instream = new DataInputStream(fstream);
		BufferedReader brstream = new BufferedReader(new InputStreamReader(instream));
		String strLine = "";
		int count = 0;
		while ((strLine = brstream.readLine()) != null) {
			this.mappingWordId.put(count++, strLine.trim());
		}
		brstream.close();
	}
	
	public void init() {
		this.dz = new int[this.doc.length][this.numOfTopic];
		for(int i = 0; i < this.doc.length; i++){
			for(int j = 0; j < this.numOfTopic; j++){
				this.dz[i][j]=0;
			}
		}
		this.dzSumZ = new int[this.doc.length];
		for(int i = 0; i < this.doc.length; i++){
			dzSumZ[i] = 0;
		}

		this.dzz = new int[this.doc.length][this.numOfTopic][this.numOfTopic];

		for(int i = 0; i < this.doc.length; i++){
			for(int j= 0; j < this.numOfTopic; j++){
				for(int k = 0; k < this.numOfTopic; k++){
					this.dzz[i][j][k] = 0;
				}
			}
		}
		this.dzzSumZ = new int[this.doc.length][this.numOfTopic];
		for(int i = 0; i < this.doc.length; i++ ){
			for(int j = 0; j < this.numOfTopic; j++){
				this.dzzSumZ[i][j] = 0;
			}
		}
		this.ze = new int[this.numOfTopic][this.mappingWordId.size()];
		for(int i = 0; i < this.numOfTopic; i++){
			for(int j= 0; j < this.mappingWordId.size(); j++){
				this.ze[i][j] = 0;
			}
		}
		this.zeSumE = new int[this.numOfTopic];
		for(int i = 0; i < this.numOfTopic; i++ ){
			this.zeSumE[i] = 0;
		}

		this.zzeeSumZZE = new int[this.numOfTopic][this.numOfTopic][this.mappingWordId.size()];
		for(int i = 0; i < this.numOfTopic; i++){
			for(int j = 0; j < this.numOfTopic; j++){
				for(int k = 0; k < this.mappingWordId.size(); k++){
					this.zzeeSumZZE[i][j][k] = 0;
				}           
			}
		}

		int count = 0;
		for (int docId = 0; docId < this.doc.length; docId++) {
			for (int wordId = 0; wordId < this.doc[docId].getDocLen(); wordId++) {
				if(!((count % 2) == 0)){
					count++;
					continue;
				}
				count++;
				int topic1 = (int) Math.floor(Math.random() * this.numOfTopic);
				int topic2 = (int) Math.floor(Math.random() * this.numOfTopic);
				this.doc[docId].setTopic(wordId, topic1);
				this.doc[docId].setTopic(wordId+1, topic2);
				int word1 = this.doc[docId].words.get(wordId);
				int word2 = this.doc[docId].words.get(wordId+1);
				
				this.dz[docId][topic1]++;
				this.dzSumZ[docId]++;
				this.ze[topic1][word1]++;
				this.zeSumE[topic1]++;  
				this.dzz[docId][topic1][topic2]++;
				this.dzzSumZ[docId][topic1]++;
				if (this.zzee.containsKey(topic1+" "+topic2 + " "+ word1 + " " + word2)) {
					int num = this.zzee.get(topic1+" "+topic2 + " "+ word1 + " " + word2);
					this.zzee.remove(topic1+" "+topic2 + " "+ word1 + " " + word2);
					this.zzee.put(topic1+" "+topic2 + " "+ word1 + " " + word2, num + 1);
				} else {
					this.zzee.put(topic1+" "+topic2 + " "+ word1 + " " + word2, 1);
				}
				this.zzeeSumZZE[topic1][topic2][word1]++;
			}
		}
	}

	public void gibSampling() {
		for (int docId = 0; docId < this.doc.length; docId++) {
			for (int wordId = 0; wordId < this.doc[docId].getDocLen(); wordId++) {
				if(!((wordId%2) == 0))continue;
				Sample(docId, wordId);   
			}
		}
	}

	public void Sample(int docId, int wordId) {
		int word1 = this.doc[docId].words.get(wordId);
		int word2 = this.doc[docId].words.get(wordId+1);
		int topic1 = this.doc[docId].topics.get(wordId);
		int topic2 = this.doc[docId].topics.get(wordId+1);
		this.dz[docId][topic1]--;
		this.dzSumZ[docId]--;
		this.ze[topic1][word1]--;
		this.zeSumE[topic1]--;
		this.dzz[docId][topic1][topic2]--;
		this.dzzSumZ[docId][topic1]--;
		if(this.zzee.containsKey(topic1+" "+topic2+" "+word1+" "+word2)){
			int tmp = this.zzee.get(topic1+" "+topic2+" "+word1+" "+word2);
			this.zzee.remove(topic1+" "+topic2+" "+word1+" "+word2);
			this.zzee.put(topic1+" "+topic2+" "+word1+" "+word2, tmp-1);
		}
		this.zzeeSumZZE[topic1][topic2][word1]--;
		int newTopic =0;
		double prob[][] = new double[this.numOfTopic][this.numOfTopic];

		for(int i = 0; i < this.numOfTopic; i++ ){
			for(int j = 0; j < this.numOfTopic; j++){
				int zzeeNum = 0;
				int zzeeSumZZENum = 0;
				zzeeSumZZENum = this.zzeeSumZZE[i][j][word1];
				if(this.zzee.containsKey(i+" "+j+" "+word1+" "+word2)){
					zzeeNum = this.zzee.get(i+" "+j+" "+word1+" "+word2);
				}
				prob[i][j] =((this.alpha + this.dz[docId][i])/(this.alpha * this.numOfTopic + this.dzSumZ[docId]))
						*((this.alphaT + this.dzz[docId][i][j]) / (this.alphaT * this.numOfTopic + this.dzzSumZ[docId][i]))
						*((this.beta+this.ze[i][word1])/(this.beta * this.mappingWordId.size() + this.zeSumE[i]))
						*((this.betaT + zzeeNum) / (this.betaT * this.mappingWordId.size() + zzeeSumZZENum));
			}
		}

		HashMap<Integer,String> mapping = new HashMap<Integer,String>();
		double CDF[] = new double[this.numOfTopic * this.numOfTopic];
		int count = 0;
		for(int i = 0; i < this.numOfTopic; i++ ){
			for(int j = 0; j < this.numOfTopic; j++){
				mapping.put(count, i+" "+j);
				CDF[count++] = prob[i][j];
			}
		}

		for(int i = 1; i < CDF.length; i++){
			CDF[i] += CDF[i-1];
		}

		double u = Math.random() * (CDF[CDF.length-1]);
		for ( newTopic = 0; newTopic < CDF.length; newTopic++) {
			if (CDF[newTopic] > u) {
				break;
			}
		}
		if (newTopic == CDF.length)
			newTopic--;     


		int newTopic1 = Integer.parseInt(mapping.get(newTopic).split(" ")[0]);
		int newTopic2 = Integer.parseInt(mapping.get(newTopic).split(" ")[1]);

		this.dz[docId][newTopic1]++;
		this.dzSumZ[docId]++;
		this.dzz[docId][newTopic1][newTopic2]++;
		this.dzzSumZ[docId][newTopic1]++;
		this.ze[newTopic1][word1]++;
		this.zeSumE[newTopic1]++;
		if(this.zzee.containsKey(newTopic1+" "+newTopic2+" "+word1+" "+word2)){
			int tmp = this.zzee.get(newTopic1+" "+newTopic2+" "+word1+" "+word2);
			this.zzee.remove(newTopic1+" "+newTopic2+" "+word1+" "+word2);
			this.zzee.put(newTopic1+" "+newTopic2+" "+word1+" "+word2, tmp+1);
		}else{
			this.zzee.put(newTopic1+" "+newTopic2+" "+word1+" "+word2, 1);
		}
		this.zzeeSumZZE[newTopic1][newTopic2][word1]++;
		this.doc[docId].setTopic(wordId, newTopic1);
		this.doc[docId].setTopic(wordId+1,newTopic2);
	}


	public void calculateTheta() throws IOException {
		FileOutputStream outfstream1 = new FileOutputStream(this.outputTheta);
		DataOutputStream out1 = new DataOutputStream(outfstream1);
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(out1));
		double[][] Theta = new double[this.doc.length][numOfTopic];
		double[] ThetaTopic = new double[this.numOfTopic];

		for (int doc = 0; doc < this.doc.length; doc++) {
			bw1.write(doc+" ");
			for (int topic = 0; topic < this.numOfTopic; topic++) {
				Theta[doc][topic] = (this.alpha + this.dz[doc][topic])/ (this.alpha * this.numOfTopic + this.dzSumZ[doc]);
				ThetaTopic[topic] += Theta[doc][topic];
				bw1.write(topic+" "+Theta[doc][topic]+" ");
			}
			bw1.write("\n");
		}          

		bw1.write("\n");
		bw1.close();
	}

	public void calculateThetaT() throws IOException {
		FileOutputStream outfstream1 = new FileOutputStream(this.outputThetaT);
		DataOutputStream out1 = new DataOutputStream(outfstream1);
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(out1));
		double ThetaT[][][] = new double[this.doc.length][this.numOfTopic][this.numOfTopic];
		double ThetaTTopic[][] = new double[this.numOfTopic][this.numOfTopic];

		for (int doc = 0; doc < this.doc.length; doc++) {
			bw1.write(doc+" ");
			for (int topic1 = 0; topic1 < this.numOfTopic; topic1++) {
				for (int topic2 = 0; topic2 < this.numOfTopic; topic2++) {
					ThetaT[doc][topic1][topic2] = (this.alphaT + this.dzz[doc][topic1][topic2])/(this.alphaT * this.numOfTopic + this.dzzSumZ[doc][topic1]);
					ThetaTTopic[topic1][topic2] += ThetaT[doc][topic1][topic2];
					bw1.write(topic1+"/"+topic2+" "+ThetaT[doc][topic1][topic2]+" ");
				}
			}
			bw1.write("\n");
		}

		for (int topic1 = 0; topic1 < this.numOfTopic; topic1++) {
			for (int topic2 = 0; topic2 < this.numOfTopic; topic2++) {
				bw1.write(topic1+"/"+topic2+" "+ThetaTTopic[topic1][topic2]+" ");
			}
		}
		bw1.write("\n");
		bw1.close();
	}


	public void calculateSavePhi() throws IOException {
		double[][] Phi = new double[this.numOfTopic][this.mappingWordId.size()];
		FileOutputStream outfstream1 = new FileOutputStream(this.outputPhi);
		DataOutputStream out1 = new DataOutputStream(outfstream1);
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(out1));
		for (int topic = 0; topic < this.numOfTopic; topic++) {

			for (int wordId = 0; wordId < this.mappingWordId.size(); wordId++) {
				Phi[topic][wordId] = (this.beta + this.ze[topic][wordId])/ (this.beta * this.mappingWordId.size() + this.zeSumE[topic]);
			}         
		}

		int countPhi = 0;

		for(int topic = 0; topic < this.numOfTopic; topic++){
			if(countPhi%1000 == 0){
				countPhi++;
			}
			List<ProbHolder> wordsProbsList = new ArrayList<ProbHolder>();
			bw1.write("topic"+topic+":"+"\n");
			for(int wordId = 0; wordId < this.mappingWordId.size(); wordId++){
				ProbHolder p = new ProbHolder(wordId, Phi[topic][wordId]);
				wordsProbsList.add(p);
			}

			Collections.sort(wordsProbsList);
			for (int i = 0; i < this.numOfTop; i++){
				if (this.mappingWordId.containsKey((Integer)wordsProbsList.get(i).obj)){

					String word = this.mappingWordId.get((Integer)wordsProbsList.get(i).obj);
					bw1.write("\t" + word + " "+ wordsProbsList.get(i).prob + "\n");
				}
			}
		}            
		bw1.close();
	}

	public void calculateSaveTopPhiT() throws IOException{
		FileOutputStream outfstream1 = new FileOutputStream(this.outputPhiT);
		DataOutputStream out1 = new DataOutputStream(outfstream1);
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(out1));
		for(int topic1 = 0; topic1 < this.numOfTopic; topic1++){
			for(int topic2 = 0; topic2 < this.numOfTopic; topic2++){
				HashSet<String> pair2Prob = new HashSet<String>();
				for(int docId = 0; docId < this.doc.length; docId++ ){
					for(int wordId = 0; wordId < this.doc[docId].getDocLen(); wordId++){
						if((wordId % 2) == 1)
							continue;
						double prob = 0.0;
						int word1 = this.doc[docId].words.get(wordId);
						int word2 = this.doc[docId].words.get(wordId+1);
						int zeNum = 0;
						int zeSumENum = 0;
						zeNum += this.ze[topic1][word1];
						zeSumENum += this.zeSumE[topic1];
						int zzeeNum = 0;
						if(this.zzee.containsKey(topic1+" "+topic2+" "+word1+" "+word2)){
							zzeeNum += this.zzee.get(topic1+" "+topic2+" "+word1+" "+word2);
						}
						int zzeeSumZZENum = 0;
						zzeeSumZZENum += this.zzeeSumZZE[topic1][topic2][word1];
						prob = ((this.beta + zeNum)/ (this.beta * this.mappingWordId.size() + zeSumENum))
								*((this.betaT + zzeeNum) / (this.betaT * this.mappingWordId.size()+zzeeSumZZENum));
						pair2Prob.add(word1 + " " + word2 + "," + prob);
					}
				}

				List<ProbHolder> wordsProbsList = new ArrayList<ProbHolder>();
				Iterator<String> iter = pair2Prob.iterator();
				while (iter.hasNext()) {
					String next = iter.next().toString();
					ProbHolder p = new ProbHolder(next.split(",")[0].toString(), Double.parseDouble(next.split(",")[1]));
					wordsProbsList.add(p);    
				}
				Collections.sort(wordsProbsList);
				bw1.write(topic1+"/"+topic2+"\n");
				for(int i = 0; i < this.numOfTop; i++){
					int word1 = Integer.parseInt(wordsProbsList.get(i).obj.toString().split(" ")[0]);
					String realWord1 = this.mappingWordId.get(word1);
					int word2 = Integer.parseInt(wordsProbsList.get(i).obj.toString().split(" ")[1]);
					String realWord2 = this.mappingWordId.get(word2);
					bw1.write(realWord1+"/"+realWord2+" "+ wordsProbsList.get(i).prob + "\n");
				}
			}
		}
		bw1.close();
	}


	public int Dictionary_new() throws IOException {
		int numOfDocNew = 0;
		HashMap<String,Integer> mappingWord = new HashMap<String,Integer>();   
		FileInputStream fstream = new FileInputStream(this.mappingFile);
		DataInputStream instream = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(instream));
		String strLineMapping = "";
		int countNew = 0;
		while((strLineMapping = br.readLine()) != null){
			if(strLineMapping.length()!=0){
				this.mappingWordIdNew.put(countNew, strLineMapping.trim());
				mappingWord.put(strLineMapping.trim(), countNew++);
			}
		}
		br.close();
		FileInputStream fstream1 = new FileInputStream(this.inputFileWordNew);
		DataInputStream instream1 = new DataInputStream(fstream1);
		BufferedReader br1 = new BufferedReader(new InputStreamReader(instream1));
		String strLine = "";

		FileOutputStream outfstream1 = new FileOutputStream(this.inputFileIdNew);
		DataOutputStream out1 = new DataOutputStream(outfstream1);
		BufferedWriter bw1 = new BufferedWriter(new OutputStreamWriter(out1));
		
		FileOutputStream outfstream2 = new FileOutputStream(this.mappingFileNew);
		DataOutputStream out2 = new DataOutputStream(outfstream2);
		BufferedWriter bw2 = new BufferedWriter(new OutputStreamWriter(out2));

		while ((strLine = br1.readLine()) != null) {
			numOfDocNew ++;
			String wordsPerDoc[] = strLine.split("&");
			for (int i = 0; i < wordsPerDoc.length; i++) {
				if (wordsPerDoc[i].length() != 0){
					if(mappingWord.containsKey(wordsPerDoc[i])){
						bw1.write(mappingWord.get(wordsPerDoc[i])+" ");
					}else{
						this.mappingWordIdNew.put(countNew, wordsPerDoc[i]);
						mappingWord.put(wordsPerDoc[i], countNew++);
						int tmp = countNew-1;
						bw1.write(tmp+" ");
					}                   
				}
			}
			bw1.write("\n");
		}
		br1.close();
		bw1.close();
		for(int i = 0; i < this.mappingWordIdNew.size(); i++){
			bw2.write(this.mappingWordIdNew.get(i)+"\n");
		}
		bw2.close();
		return numOfDocNew;
	}

	public void loadDocument_new(int numOfDocNew) throws IOException {
		this.docNew = new Document[numOfDocNew];
		FileInputStream fstream = new FileInputStream(this.inputFileIdNew);
		DataInputStream instream = new DataInputStream(fstream);
		BufferedReader brstream = new BufferedReader(new InputStreamReader(
				instream));
		String strLine = "";
		int countDoc = 0;
		while ((strLine = brstream.readLine()) != null) {
			String wordsPerDoc[] = strLine.split(" ");
			this.docNew[countDoc++] = new Document(wordsPerDoc);
		}
		brstream.close();
	}

	public void init_new() {   
		this.dzNew = new int[this.docNew.length][this.numOfTopic];
		for(int i = 0; i < this.docNew.length; i++){
			for(int j = 0; j < this.numOfTopic; j++){
				this.dzNew[i][j]=0;
			}
		}
		this.dzSumZNew = new int[this.docNew.length];
		for(int i = 0; i < this.docNew.length; i++){
			this.dzSumZNew[i] = 0;
		}

		this.dzzNew = new int[this.docNew.length][this.numOfTopic][this.numOfTopic];
		for(int i = 0; i < this.docNew.length; i++){
			for(int j= 0; j < this.numOfTopic; j++){
				for(int k = 0; k < this.numOfTopic; k++){
					this.dzzNew[i][j][k] = 0;
				}
			}
		}
		this.dzzSumZNew = new int[this.docNew.length][this.numOfTopic];
		for(int i = 0; i < this.docNew.length; i++ ){
			for(int j = 0; j < this.numOfTopic; j++){
				this.dzzSumZNew[i][j] = 0;
			}
		}

		this.zeNew = new int[this.numOfTopic][this.mappingWordIdNew.size()];
		for(int i = 0; i < this.numOfTopic; i++){
			for(int j= 0; j < this.mappingWordIdNew.size(); j++){
				this.zeNew[i][j] = 0;
			}
		}

		this.zeSumENew = new int[this.numOfTopic];
		for(int i = 0; i < this.numOfTopic; i++ ){
			this.zeSumENew[i] = 0;
		}
		this.zzeeSumZZENew = new int[this.numOfTopic][this.numOfTopic][this.mappingWordIdNew.size()];
		for(int i = 0; i < this.numOfTopic; i++){
			for(int j = 0; j < this.numOfTopic; j++){
				for(int k = 0; k < this.mappingWordIdNew.size(); k++){
					this.zzeeSumZZENew[i][j][k] = 0;
				}
			}
		}

		int count = 0;
		for (int docId = 0; docId < this.docNew.length; docId++) {
			
			for (int wordId = 0; wordId < this.docNew[docId].getDocLen(); wordId++) {
				if(!((count % 2) == 0)){
					count++;
					continue;
				}
				count++;
				int topic1 = (int) Math.floor(Math.random() * this.numOfTopic);
				int topic2 = (int) Math.floor(Math.random() * this.numOfTopic);
				this.docNew[docId].setTopic(wordId, topic1);
				this.docNew[docId].setTopic(wordId+1, topic2);
				int word1 = this.docNew[docId].words.get(wordId);
				int word2 = this.docNew[docId].words.get(wordId+1);
				this.dzNew[docId][topic1]++;
				this.dzSumZNew[docId]++;
				this.dzzNew[docId][topic1][topic2]++;
				this.dzzSumZNew[docId][topic1]++;
				this.zeNew[topic1][word1]++;
				this.zeSumENew[topic1]++;   
				if (this.zzeeNew.containsKey(topic1+" "+topic2 + " "+ word1 + " " + word2)) {
					int num = this.zzeeNew.get(topic1+" "+topic2 + " "+ word1 + " " + word2);
					this.zzeeNew.remove(topic1+" "+topic2 + " "+ word1 + " " + word2);
					this.zzeeNew.put(topic1+" "+topic2 + " "+ word1 + " " + word2, num + 1);
				} else {
					this.zzeeNew.put(topic1+" "+topic2 + " "+ word1 + " " + word2, 1);
				}
				this.zzeeSumZZENew[topic1][topic2][word1]++;
			}
		}
	}

	public void gibSampling_new() {
		for (int docId = 0; docId < this.docNew.length; docId++) {
			for (int wordId = 0; wordId < this.docNew[docId].getDocLen(); wordId++) {
				if(!((wordId%2) == 0)){
					continue;
				}
				Sample_new(docId, wordId);
			}
		}
	}

	public void Sample_new(int docId, int wordId) {
		int word1 = this.docNew[docId].words.get(wordId);
		int word2 = this.docNew[docId].words.get(wordId+1);
		int topic1 = this.docNew[docId].topics.get(wordId);
		int topic2 = this.docNew[docId].topics.get(wordId+1);
		this.dzNew[docId][topic1]--;
		this.dzSumZNew[docId]--;
		this.dzzNew[docId][topic1][topic2]--;
		this.dzzSumZNew[docId][topic1]--;
		this.zeNew[topic1][word1]--;
		this.zeSumENew[topic1]--;
		if(this.zzeeNew.containsKey(topic1+" "+topic2+" "+word1+" "+word2)){
			int tmp = this.zzeeNew.get(topic1+" "+topic2+" "+word1+" "+word2);
			this.zzeeNew.remove(topic1+" "+topic2+" "+word1+" "+word2);
			this.zzeeNew.put(topic1+" "+topic2+" "+word1+" "+word2, tmp-1);
		}
		this.zzeeSumZZENew[topic1][topic2][word1]--;
		int newTopic =0;
		double prob[][] = new double[this.numOfTopic][this.numOfTopic];
		for(int i = 0; i < this.numOfTopic; i++ ){
			for(int j = 0; j < this.numOfTopic; j++){
				int zzeeNum = 0;
				if(this.zzeeNew.containsKey(i + " " + j + " " + word1 + " " + word2)){
					zzeeNum += this.zzeeNew.get(i + " " + j + " " + word1 + " " + word2);
				}   
				if(this.zzee.containsKey(i + " " + j + " " + word1 + " " + word2)){
					zzeeNum += this.zzee.get(i + " " + j + " " + word1 + " " + word2);
				}
				int zzeeSumZZENum = 0;
				if(this.mappingWordId.containsKey(word1)){
					zzeeSumZZENum += this.zzeeSumZZE[i][j][word1] + this.zzeeSumZZENew[i][j][word1];
				}else{
					zzeeSumZZENum += this.zzeeSumZZENew[i][j][word1];
				}
				int zeNum = 0;
				int zeSumENum = 0;
				if(this.mappingWordId.containsKey(word1))
					zeNum += this.ze[i][word1];
				zeNum += this.zeNew[i][word1];
				zeSumENum += this.zeSumE[i] + this.zeSumENew[i];
				prob[i][j] =((this.alpha + this.dzNew[docId][i])/(this.alpha * this.numOfTopic + this.dzSumZNew[docId]))
						*((this.alphaT + this.dzzNew[docId][i][j]) / (this.alphaT * this.numOfTopic + this.dzzSumZNew[docId][i]))
						*((this.beta + zeNum)/(this.beta * this.mappingWordIdNew.size()+zeSumENum))                       
						*((this.betaT + zzeeNum) / (this.betaT * this.mappingWordIdNew.size() + zzeeSumZZENum));   
			}
		}

		HashMap<Integer,String> mapping = new HashMap<Integer,String>();
		double CDF[] = new double[this.numOfTopic * this.numOfTopic];
		int count = 0;
		for(int i = 0; i < this.numOfTopic; i++ ){
			for(int j = 0; j < this.numOfTopic; j++){
				mapping.put(count, i+" "+j);
				CDF[count++] = prob[i][j];
			}
		}

		for(int i = 1; i < CDF.length; i++){
			CDF[i] += CDF[i-1];
		}

		double u = Math.random() * (CDF[CDF.length-1]);
		for (newTopic = 0; newTopic < CDF.length; newTopic++) {
			if (CDF[newTopic] > u) {
				break;
			}
		}
		if (newTopic == CDF.length)
			newTopic--;     

		int newTopic1 = Integer.parseInt(mapping.get(newTopic).split(" ")[0]);
		int newTopic2 = Integer.parseInt(mapping.get(newTopic).split(" ")[1]);

		this.dzNew[docId][newTopic1]++;
		this.dzSumZNew[docId]++;
		this.zeNew[newTopic1][word1]++;
		this.zeSumENew[newTopic1]++;
		this.dzzNew[docId][newTopic1][newTopic2]++;
		this.dzzSumZNew[docId][newTopic1]++;
		if(this.zzeeNew.containsKey(newTopic1+" "+newTopic2+" "+word1+" "+word2)){
			int tmp = this.zzeeNew.get(newTopic1+" "+newTopic2+" "+word1+" "+word2);
			this.zzeeNew.remove(newTopic1+" "+newTopic2+" "+word1+" "+word2);
			this.zzeeNew.put(newTopic1+" "+newTopic2+" "+word1+" "+word2, tmp+1);
		}else{
			this.zzeeNew.put(newTopic1+" "+newTopic2+" "+word1+" "+word2, 1);
		}
		this.zzeeSumZZENew[newTopic1][newTopic2][word1]++;
		this.docNew[docId].setTopic(wordId, newTopic1);
		this.docNew[docId].setTopic(wordId+1,newTopic2);
	}

	public double calculatePerplexity(){
		double logLik = 0;
		for(int docId = 0; docId < this.docNew.length; docId++ ){
			for(int wordId = 0; wordId < this.docNew[docId].getDocLen(); wordId++){
				double sum = 0.0;
				if((wordId % 2) == 1)
					continue;             
				int word1 = this.docNew[docId].words.get(wordId);
				int word2 = this.docNew[docId].words.get(wordId+1);
				for(int topic1 = 0; topic1 < this.numOfTopic; topic1++){
					for(int topic2 = 0; topic2 < this.numOfTopic; topic2++){
						int zeNum = 0;
						int zeSumENum = 0;
						if(this.mappingWordId.containsKey(word1)){
							zeNum += this.ze[topic1][word1] + this.zeNew[topic1][word1];
						}else{
							zeNum += this.zeNew[topic1][word1];
						}
						zeSumENum += this.zeSumE[topic1] + this.zeSumENew[topic1];
						int zzeeNum = 0;
						if(this.zzeeNew.containsKey( topic1 + " " + topic2 + " " + word1 + " " + word2)){
							zzeeNum += this.zzeeNew.get(topic1 + " " + topic2 + " " + word1 + " " + word2);
						}   
						if(this.zzee.containsKey(topic1 + " " + topic2 + " " + word1 + " " + word2)){
							zzeeNum += this.zzee.get(topic1 + " " + topic2+ " " + word1 + " " + word2);
						}
						int zzeeSumZZENum = 0;
						if(this.mappingWordId.containsKey(word1)){
							zzeeSumZZENum += this.zzeeSumZZE[topic1][topic2][word1] + this.zzeeSumZZENew[topic1][topic2][word1];
						}else{
							zzeeSumZZENum += this.zzeeSumZZENew[topic1][topic2][word1];
						}

						sum += ((this.alpha + this.dzNew[docId][topic1])/(this.alpha * this.numOfTopic + this.dzSumZNew[docId]))
								*((this.alphaT + this.dzzNew[docId][topic1][topic2])/(this.alphaT * this.numOfTopic + this.dzzSumZNew[docId][topic1]))
								*((this.beta + zeNum)/ (this.beta * this.mappingWordIdNew.size() + zeSumENum))
								*((this.betaT + zzeeNum) / (this.betaT * this.mappingWordIdNew.size()+zzeeSumZZENum));

					}
				}
				logLik += Math.log(sum);
			}
		}
		return Math.exp(-logLik/(this.docNew.length));
	}

	public void run_estimate() throws IOException {
		System.out.println("Estimation ...");
		System.out.println("creating dictionary...");
		int numOfDoc = this.Dictionary();
		this.mapping();
		System.out.println("loading documents...");
		this.loadDocument(numOfDoc);
		System.out.println("model initializing...");
		this.init();
		System.out.println("Sampling...");
		for(int i = 0; i < numOfIter; i++){
			System.out.println("The " + i + "th iteration...");
			gibSampling();
		}    

		this.calculateTheta();
		this.calculateThetaT();
		this.calculateSavePhi();
		this.calculateSaveTopPhiT();
	}
	
	public double run_inference() throws IOException {
		System.out.println("\n" + "Inference ...");
		int numOfDocNew = this.Dictionary_new();
		this.loadDocument_new(numOfDocNew);
		this.init_new();
		for(int i = 0; i < numOfIter; i++){
			gibSampling_new();
		}    
		return calculatePerplexity();
	}
	
    public static void main(String args[]) throws IOException{
        PairwiseTopicModel model = new PairwiseTopicModel ();
        model.run_estimate();
        double perplexity = model.run_inference();
        System.out.println("perplexity: " + perplexity);
    }
}