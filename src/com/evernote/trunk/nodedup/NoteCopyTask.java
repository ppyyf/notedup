package com.evernote.trunk.nodedup;
import com.evernote.edam.type.Note;

public class NoteCopyTask {
	private final Note note;
	private final String notebookName;
	private final int totalNoteNumberInCurrentNotebook;
	private final int currentNoteNumberInCurrentNotebook;
	private final int totalNotebookNumber;
	private final int currentNotebookNumber;
	
	public NoteCopyTask(Note note, String notebookName, int totalNoteNumberInCurrentNotebook,int currentNoteNumberInCurrentNotebook,int totalNotebookNumber,int currentNotebookNumber){
		this.note = note;
		this.notebookName = notebookName;
		this.totalNoteNumberInCurrentNotebook = totalNoteNumberInCurrentNotebook;
		this.currentNoteNumberInCurrentNotebook = currentNoteNumberInCurrentNotebook;
		this.totalNotebookNumber = totalNotebookNumber;
		this.currentNotebookNumber = currentNotebookNumber;
	}
	public Note getNote() {
		return note;
	}
	public String getNotebookName() {
		return notebookName;
	}
	public int getTotalNoteNumberInCurrentNotebook() {
		return totalNoteNumberInCurrentNotebook;
	}
	public int getCurrentNoteNumberInCurrentNotebook() {
		return currentNoteNumberInCurrentNotebook;
	}
	public int getTotalNotebookNumber() {
		return totalNotebookNumber;
	}
	public int getCurrentNotebookNumber() {
		return currentNotebookNumber;
	}
}
