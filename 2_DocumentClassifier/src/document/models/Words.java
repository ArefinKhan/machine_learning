package document.models;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import document.database.DBConnection;

public class Words {

	public static final String TABLE_NAME = "words";
	
	public static final String ID = "_id";
	long id;
	
	public static final String WORD = "word";
	String word;
	
	public static final String TOPIC_ID = "topic_id";
	long topic_id;
	
	public static final String DOCUMENT_ID = "document_id";
	long document_id;
	
	public Words() {}

	public Words(long id, String word, long topic_id, long document_id) {
		super();
		this.id = id;
		this.word = word;
		this.topic_id = topic_id;
		this.document_id = document_id;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public long getTopic_id() {
		return topic_id;
	}

	public void setTopic_id(long topic_id) {
		this.topic_id = topic_id;
	}

	public long getDocument_id() {
		return document_id;
	}

	public void setDocument_id(long document_id) {
		this.document_id = document_id;
	}
	
	public static long insert(Connection con, String word,long topicid,long documentid)
	{
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("INSERT INTO ").append(TABLE_NAME);
		queryBuilder.append(" (").append(WORD).append(",");
		queryBuilder.append(TOPIC_ID).append(",").append(DOCUMENT_ID).append(") ");
		queryBuilder.append("VALUES ").append("(?,?,?)");
	
		try {
			PreparedStatement preparedStatement = con.prepareStatement(queryBuilder.toString());
			queryBuilder.setLength(0);
			preparedStatement.setString(1, word);
			preparedStatement.setLong(2, topicid);
			preparedStatement.setLong(3, documentid);
			long res = preparedStatement .executeUpdate();
			return res;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return -1;
	}
	
	public static int getWordFrequencyByTopic(Connection c,String word,int topic)
	{
		int count = 0;
		String sql = "SELECT COUNT(" + WORD + ") FROM " + TABLE_NAME + " WHERE " + WORD + "=? AND " + TOPIC_ID + "=?";
		try
		{
			PreparedStatement pst = c.prepareStatement(sql);
			pst.setString(1, word);
			pst.setInt(2, topic);
			ResultSet rs = pst.executeQuery();
			while(rs.next())
			{
				count = rs.getInt(1);
				break;
			}
			rs.close();
			pst.close();
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		return count;
	}
	
	public static int getWordCountByTopic(Connection c , int topic)
	{
		int count = 0;
		String sql = "SELECT COUNT(" + WORD + ") FROM " + TABLE_NAME + " WHERE " + TOPIC_ID + "=?";
		try
		{
			PreparedStatement pst = c.prepareStatement(sql);
			pst.setInt(1, topic);
			ResultSet rs = pst.executeQuery();
			while(rs.next())
			{
				count = rs.getInt(1);
				break;
			}
			rs.close();
			pst.close();
	
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		return count;
	}
}
