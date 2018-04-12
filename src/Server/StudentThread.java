package Server;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

import Data.Assignment;
import Data.AssignmentFileContainer;
import Data.Constants;
import Data.Course;
import Data.Email;
import Data.FileContainer;
import Data.Dropbox;
import Data.Grade;
import Data.StudentEnrollment;
import Data.SubmissionFileContainer;
import Database.DatabaseHelper;

public class StudentThread implements Constants {
	private String operation; 
	private DatabaseHelper database;
	
	private ObjectOutputStream objectOut; 
	
	//input stream to receive messages from client 
	private ObjectInputStream objectIn;
	
	private BufferedReader stringIn; 
	
	private PrintWriter stringOut;
	
	private Boolean checkEnd;
	
	public StudentThread(BufferedReader sIn, PrintWriter sOut, ObjectOutputStream oOut, ObjectInputStream oIn, DatabaseHelper data) {
		stringIn = sIn; 
		stringOut = sOut;
		objectOut = oOut; 
		objectIn = oIn;
		database = data;
		checkEnd = false;
	}
	
	public void run() {
		while (true) {
			try {
				operation = stringIn.readLine();
				System.out.println(operation);
				Thread.sleep(50);
				readOperation(); 
				if (checkEnd) {
					return;
				}
			}
			catch (IOException ex) {
				ex.printStackTrace();
			} 
			catch(InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void readOperation() {
		if (operation.equals(STUDENT_COURSES)) {
			getStudentCourses(); 
		}
		else if (operation.equals(GET_ASSIGN)) {
			getStudentAssignments();
		}
		else if (operation.equals(DOWNLOAD_ASSIGN)) { 
			downloadAssignment(); 
		}
		else if (operation.equals(SUBMIT_ASSIGN)) {
			submitAssignment(); 
		}
		else if (operation.equals(GET_GRADES)) {
			getGrades();
		}
		else if (operation.equals(SEND_EMAIL)) {
			sendEmail(); 
		}
		else if (operation.equals(EXIT)) {
			exitThread(); 
		}
		else {
			System.out.println("wrong operation");
		}
	}
	public void getStudentCourses() {
		try {
			String id = stringIn.readLine();
			ArrayList<Integer> courseIdList = database.searchCoursesForStudent(Integer.parseInt(id));
			ArrayList<Course> courseList = new ArrayList<Course>(); 
			for (int i = 0; i < courseIdList.size(); i++) {
				courseList.add(database.searchCourse(courseIdList.get(i)));
			}
			for(int i = 0 ; i < courseList.size(); i++)
			{
				if(courseList.get(i).getActive() ==  false)
				{
					courseList.remove(i);
					i--;
				}
			}
			objectOut.flush();
			objectOut.writeObject(courseList);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public  void getStudentAssignments() { 
		try {
			Course c = (Course)objectIn.readObject();
			 System.out.println("Got course on server");
			ArrayList<Assignment> a = database.assignmentList(c.getId());
			for(int i = 0 ; i < a.size(); i++)
			{
				if(a.get(i).getActive() ==  false)
				{
					a.remove(i);
					i--;
				}
			}
			objectOut.flush();
			objectOut.writeObject(a);
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void downloadAssignment() { 
		
		try {
			Assignment assign = (Assignment)objectIn.readObject();
			
			AssignmentFileContainer fileContainer = database.getAssignFile(assign);
			
			objectOut.flush();
			objectOut.writeObject(fileContainer);
		} 	
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void submitAssignment() { 
		try {
			SubmissionFileContainer container = (SubmissionFileContainer)objectIn.readObject();
			byte[] content = container.getFileArr();
			
			File newFile = new File("submissions/" + container.getFileName());
			
			if(!newFile.exists())
			{
				newFile.createNewFile();
			}
			FileOutputStream writer = new FileOutputStream(newFile);
			BufferedOutputStream bos = new BufferedOutputStream(writer);
			bos.write(content);
			bos.close();
			
			Dropbox submission = container.getSubmission();
			submission.setPath("submissions/" + container.getFileName());
			
			Random rand = new Random();
			submission.setId(rand.nextInt(9999));
			database.addSubmission(submission);
		}
		catch (ClassNotFoundException e) 
		{
			e.printStackTrace();
		}	
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void getGrades() { 
		try {
			StudentEnrollment c = (StudentEnrollment)objectIn.readObject();
			ArrayList<Assignment> grades = database.assignmentList(c.getCourseId());
			ArrayList<Dropbox> d = new ArrayList<Dropbox>();
			for(int i = 0 ; i< grades.size(); i++)
			{
				d.add(database.getGrades(c.getStudentId(), grades.get(i)));
			}
			objectOut.flush();
			objectOut.writeObject(d);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void sendEmail() {
		try {
			Email email = (Email)objectIn.readObject();		
			ArrayList<String> emailList = new ArrayList<String>();
			emailList.add(database.getProfessorEmail(email.getCourse().getProfId()));
			
			EmailHelper emailHelper = new EmailHelper(email, emailList);
			boolean messageSent = emailHelper.sendEmail();
			if(messageSent) {
				stringOut.println("MESSAGE_SENT");
			}
			else {
				stringOut.println("MESSAGE_FAILED");
			}
		}
		catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public void exitThread() {
		try {
			objectOut.close();
			objectIn.close();
			stringIn.close();
			stringOut.close();
			checkEnd = true;
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
