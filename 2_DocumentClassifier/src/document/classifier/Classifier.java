package document.classifier;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import utils.WordUtils;

import document.database.*;
import document.models.Documents;
import document.models.TestWords;
import document.models.Topics;
import document.models.Words;

public class Classifier {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//clearDatabase();
		WordUtils.listPrepositions();
		/*
		 * precalculation
		 */
		//readTopics();
		//readTrainingData();
		
		/*
		 * on the fly
		 */
		calculatePosteriors();
		readTestData();
	}
	
	private static void calculatePosteriors()
	{
		PosteriorCalculation.init();
		PosteriorCalculation.calculatePosteriorTopicProbability();
	}
	
	private static void clearDatabase(Connection c)
	{
		Statement stmt = null;
		try {
			
			stmt = c.createStatement();
			String sql1 = "DELETE FROM " + TestWords.TABLE_NAME;
			stmt.executeUpdate(sql1);
			stmt.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	private static void readTopics()
	{
		Statement stmt = null;
		Connection c = DBConnection.getConnection();
		try (BufferedReader br = new BufferedReader(new FileReader("topics.data")))
		{
			String sCurrentLine;
			c.setAutoCommit(false);
			while ((sCurrentLine = br.readLine()) != null) 
			{
				//System.out.println(sCurrentLine);
				stmt = c.createStatement();
				StringBuilder sqlBuilder = new StringBuilder();
				sqlBuilder.append("INSERT INTO ");
				sqlBuilder.append(Topics.TABLE_NAME).append(" ");
				sqlBuilder.append("( ").append(Topics.NAME).append(" ) ");
				sqlBuilder.append("VALUES ");
				sqlBuilder.append("('").append(sCurrentLine).append("')");
				String sql = sqlBuilder.toString();
				stmt.executeUpdate(sql);
				stmt.close();
				sqlBuilder.setLength(0);
			}
			c.commit();
			if(c!=null) {
				c.setAutoCommit(true); 
				c.close();
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Done");
	}
	
	private static void readTrainingData()
	{
		
		
		Statement stmt = null;
		Connection c = DBConnection.getConnection();
		PreparedStatement pstmt = null;
		
		try (BufferedReader br = new BufferedReader(new FileReader("training.data")))
		{
			String sCurrentLine;
			c.setAutoCommit(false);
			boolean isTopic,isTitle,isLocation,isStory,blankAfterTitle,blankAfterStory;
			isTopic = isTitle = isLocation = isStory = blankAfterTitle = false;
			blankAfterStory = true;
			int countBlanks = 0;
			String[] currLine;
			long documentid = -1;
			long topicid = -1;
			int len;
			while ((sCurrentLine = br.readLine()) != null) 
			{
				len = sCurrentLine.length();
				
				//System.out.println(sCurrentLine);
				if(sCurrentLine.isEmpty())
				{
					countBlanks++;
					if(isStory || countBlanks >=2) 
					{
						isTopic = isTitle = isLocation = isStory = false;
					}
					if(countBlanks >= 2) blankAfterStory = true;
					if(isTitle) blankAfterTitle = true;
					continue;
				}
				else
				{
					currLine = splitLine(sCurrentLine);
					countBlanks = 0;
					if(currLine.length == 1 && !isTopic && blankAfterStory)
					{
						isTopic = true;
						blankAfterStory = blankAfterTitle = false;
						//stmt = c.createStatement();
						
						StringBuilder sqlBuilder = new StringBuilder();
						sqlBuilder.append("SELECT * FROM ").append(Topics.TABLE_NAME);
						sqlBuilder.append(" WHERE ").append(Topics.NAME).append("=?");
						String sql = sqlBuilder.toString();
						sqlBuilder.setLength(0);
						pstmt = c.prepareStatement(sql);
						pstmt.setString(1, sCurrentLine);
						ResultSet rs = pstmt.executeQuery();
						//stmt.close();
						while(rs.next())
						{
							topicid = rs.getLong(Topics.ID);
							break;
						}
						rs.close();
						pstmt.close();
						
						stmt = c.createStatement();
						sqlBuilder.setLength(0);
						sqlBuilder.append("INSERT INTO ").append(Documents.TABLE_NAME);
						sqlBuilder.append(" (").append(Documents.TOPICS_ID).append(")");
						sqlBuilder.append(" VALUES ");
						sqlBuilder.append("(").append(topicid).append(")");
						sql = sqlBuilder.toString();
						
						long wow = stmt.executeUpdate(sql);
						stmt.close();
						if(wow > 0)
						{
							stmt = c.createStatement();
							rs = stmt.executeQuery("SELECT last_insert_rowid()");
							while(rs.next())
							{
								documentid = rs.getLong(1);
								break;
							}
							rs.close();
							stmt.close();
						}
						//System.out.println(wow);
					}
					else if(sCurrentLine.charAt(len - 1) == '-' && !isLocation)
					{
						isLocation = true;
						
						for (String word : currLine) 
						{
							String modified = WordUtils.cleanWord(word.toLowerCase().trim());
							if(modified.isEmpty()) continue;
							else if(WordUtils.isPreposition(modified)) continue;
							Words.insert(c , modified, topicid, documentid);
						}
						
					}
					else
					{
						if(!blankAfterTitle)
						{
							// still getting title
							isTitle = true;
							
							for (String word : currLine) 
							{
								String modified = WordUtils.cleanWord(word.toLowerCase().trim());
								if(modified.isEmpty()) continue;
								else if(WordUtils.isPreposition(modified)) continue;
								Words.insert(c , modified, topicid, documentid);
							}
							
						}
						else
						{
							// it must be story
							isStory = true;
							
							for (String word : currLine) 
							{
								String modified = WordUtils.cleanWord(word.toLowerCase().trim());
								if(modified.isEmpty()) continue;
								else if(WordUtils.isPreposition(modified)) continue;
								Words.insert(c , modified, topicid, documentid);
							}
						}
					}
				}

			}
			
			c.commit();
			if(c!=null) {
				c.setAutoCommit(true); 
				c.close();
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Reading Training Data Done");
	}
	
	private static void readTestData()
	{
		Connection c = DBConnection.getConnection();
		try (BufferedReader br = new BufferedReader(new FileReader("test.data")))
		{
			String sCurrentLine;
			c.setAutoCommit(false);
			boolean isTopic,isTitle,isLocation,isStory,blankAfterTitle,blankAfterStory;
			isTopic = isTitle = isLocation = isStory = blankAfterTitle = false;
			blankAfterStory = true;
			int countBlanks = 0;
			String[] currLine;
			long documentid = -1;
			long topicid = -1;
			int len;
			String givenTopic = "";
			while ((sCurrentLine = br.readLine()) != null) 
			{
				len = sCurrentLine.length();
				
				//System.out.println(sCurrentLine);
				if(sCurrentLine.isEmpty())
				{
					countBlanks++;
					if(isStory || countBlanks >=2) 
					{
						isTopic = isTitle = isLocation = isStory = false;
					}
					if(countBlanks >= 2) blankAfterStory = true;
					if(isTitle) blankAfterTitle = true;
					continue;
				}
				else
				{
					currLine = splitLine(sCurrentLine);
					countBlanks = 0;
					if(currLine.length == 1 && !isTopic && blankAfterStory)
					{
						isTopic = true;
						blankAfterStory = blankAfterTitle = false;
						
						if(!givenTopic.isEmpty())
						{
							/*
							 * take decision on previous test document
							 */
							//System.out.println("Before classification");
							//classify(c , givenTopic);
							givenTopic = sCurrentLine;
							//clearDatabase(c);
							//System.out.println(sCurrentLine);
						}
						else
						{
							/*
							 * this is the first document
							 */
							givenTopic = sCurrentLine;
						}
						
					}
					else if(sCurrentLine.charAt(len - 1) == '-' && !isLocation)
					{
						isLocation = true;
						
						for (String word : currLine) 
						{
							String modified = WordUtils.cleanWord(word.toLowerCase().trim());
							//System.out.println("location " + modified);
							if(modified.isEmpty()) continue;
							else if(WordUtils.isPreposition(modified)) continue;
							TestWords.insert(c , modified, topicid, documentid);
						}
						
					}
					else
					{
						if(!blankAfterTitle)
						{
							// still getting title
							isTitle = true;
							
							for (String word : currLine) 
							{
								String modified = WordUtils.cleanWord(word.toLowerCase().trim());
								//System.out.println("title " + modified);
								if(modified.isEmpty() || modified==null) continue;
								else if(WordUtils.isPreposition(modified)) continue;
								TestWords.insert(c , modified, topicid, documentid);
							}
							
						}
						else
						{
							// it must be story
							isStory = true;
							
							for (String word : currLine) 
							{
								String modified = WordUtils.cleanWord(word.toLowerCase().trim());
								//System.out.println("story " + modified);
								if(modified.isEmpty() || modified==null) continue;
								else if(WordUtils.isPreposition(modified)) continue;
								TestWords.insert(c , modified, topicid, documentid);
							}
						}
					}
				}

			}
			c.commit();
			if(c!=null) {
				c.setAutoCommit(true); 
				c.close();
			}
		} catch (IOException | SQLException e) {
			e.printStackTrace();
		}
		System.out.println("Test Data Reading Done");
	}
	
	private static String[] splitLine(String line)
	{
		String[] splitStr = line.split("\\s+");
		return splitStr;
	}
	
	private static void classify(Connection c,String topic)
	{
		double max = 0.0;
		double val = 0.0;
		while(true)
		{
			ArrayList<String> words = TestWords.getDistinctWords(c);
			//System.out.println("words size " + words.size());
			int wordFrequencyByTopic;
			int wordCountByTopic;
			double probability,mult,product;
			int whichTopic = 0;
			for(int i=1; i<=PosteriorCalculation.getTotalTopics(); i++)
			{
				//System.out.println("topic " + i);
				val = 1.0;
				val *= PosteriorCalculation.getTopicProbability(i);
				if(val == 0.0) continue;
				wordCountByTopic = Words.getWordCountByTopic(c , i);
				for (String word : words) 
				{
					//System.out.println("word " + word);
					wordFrequencyByTopic = Words.getWordFrequencyByTopic(c , word, i);
					probability = (double)(wordFrequencyByTopic+1) / (wordCountByTopic + PosteriorCalculation.getTotalVocabulary());
					//System.out.println(probability + " " + wordFrequencyByTopic + " " + wordCountByTopic);
					product = 1.0;
					int frequency = TestWords.getFrequency(c , word);
					for(int j=1; j<=frequency; j++)
					{
						product *= probability;
					}
					val *= product;
				}
				if(Double.compare(val, max)>0)
				{
					max = val;
					whichTopic = i;
					System.out.println("one max found " + i + " " + max);
				}
			}
			System.out.println("Given:"+topic + " decision:"+whichTopic);
			break;
		}
	}
}
