package com.evernote.trunk.nodedup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.evernote.edam.type.LazyMap;
import com.evernote.edam.type.Note;

public class NoteCopySlave extends Thread {
	private Account source;
	private Account target;
	private final String sourceTag;
	private final String targetTag;
	private final CopyState copyState;
	private final BlockingQueue<NoteCopyTask> noteQueue;
	private String currentState;
	private boolean done;
	private NoteCopyTask lastTask;
	private final int stateMaxNotebookName = 15;
	private final int stateMaxNoteTitle = 15;
	private final String stateFormat = "[%s] [%s(%s/%s)] [%s(%s/%s)] [%s]"; //$NON-NLS-1$
	
	public NoteCopySlave(Account source, Account target, final String sourceTag, final String targetTag, BlockingQueue<NoteCopyTask> noteQueue, CopyState copyState) {
		this.source = source;
		this.target = target;
		this.sourceTag = sourceTag;
		this.targetTag = targetTag;
		this.copyState = copyState;
		this.noteQueue = noteQueue;
		this.setCurrentState(Messages.getString("NoteCopySlave.initializing"), null, 0); //$NON-NLS-1$
		this.done = false;
		this.lastTask = null;
	}

	@Override
	public void run(){
		this.source = this.source.clone();
		this.target = this.target.clone();
		NoteCopyTask task;
		// retry last failed task first
		if (!copyState.isStopped() && this.lastTask !=null && this.lastTask.getNote()!=null){
			this.setCurrentState(Messages.getString("NoteCopySlave.waiting.for.30.seconds"), null, 0); //$NON-NLS-1$
			try {
				Thread.sleep(30000);
			} catch (InterruptedException e) {
				if (copyState.isStopped()){
					this.setCurrentState(Messages.getString("NoteCopySlave.stopped"), null, 0); //$NON-NLS-1$
				}
			}
			if (!copyState.isStopped()){
				this.copyNote(this.lastTask);
			}
		}
		this.setCurrentState(Messages.getString("NoteCopySlave.waiting"), null, 0); //$NON-NLS-1$
		while(true){
			if (copyState.isStopped()){
				this.setCurrentState(Messages.getString("NoteCopySlave.stopped"), null, 0); //$NON-NLS-1$
				break;
			}
			try {
				task = noteQueue.take();
				this.lastTask = task;
				if (task.getNote() == null){
					while(true){
						try{
							noteQueue.put(task);
							break;
						} catch (InterruptedException e) {
							// ignore this interruption and keep trying to add ending task.
						}
					}
					this.setCurrentState(Messages.getString("NoteCopySlave.done"), null, 0); //$NON-NLS-1$
					break;
				}
			} catch (InterruptedException e) {
				if (copyState.isStopped()){
					this.setCurrentState(Messages.getString("NoteCopySlave.stopped"), null, 0); //$NON-NLS-1$
					break;
				}
				continue;
			}
			// copy note to target account
			long noteSize = Account.noteSize(task.getNote());
			copyState.subtractQueueSize(noteSize);
			if (copyState.testAndAddUploadedSize(noteSize)){
				copyNote(task);
			} else {
				copyState.addNotcopied();
			}
			this.setCurrentState(Messages.getString("NoteCopySlave.waiting"), null, 0); //$NON-NLS-1$
			System.gc();
		}
		this.lastTask = null;
		this.done = true;
	}

	private void copyNote(NoteCopyTask task) {
		long noteSize = Account.noteSize(task.getNote());
		this.setCurrentState(Messages.getString("NoteCopySlave.loading"), task, noteSize); //$NON-NLS-1$
		Note srcNote = source.getNote(task.getNote().getGuid());
		srcNote.getAttributes().unsetShareDate();
		this.setCurrentState(Messages.getString("NoteCopySlave.tagging"), task, noteSize); //$NON-NLS-1$
		if(srcNote.getTagGuidsSize()>0){
			List<String> tagNames = new LinkedList<String>();
			for(String tagGuid : srcNote.getTagGuids()){
				if (source.getTagByGuid(tagGuid)!=null){
					tagNames.add(source.getTagByGuid(tagGuid).getName());
				} else {
					tagNames.add(source.getTag(tagGuid).getName());
				}
			}
			srcNote.setTagNames(tagNames);
			srcNote.setTagGuids(null);
		}
		
        // save original NOTE GUID in applicationData for note link correction.
		if (srcNote.getAttributes().getApplicationData() == null){
			srcNote.getAttributes().setApplicationData(new LazyMap());
			srcNote.getAttributes().getApplicationData().setFullMap(new HashMap<String,String>());
		} else if (srcNote.getAttributes().getApplicationData().getFullMap() == null){
			srcNote.getAttributes().getApplicationData().setFullMap(new HashMap<String,String>());
		}
		srcNote.getAttributes().getApplicationData().getFullMap().put(AuthHelperSystemBrowser.getKey(),source.getUserId()+"/"+source.getShardId()+"/"+srcNote.getGuid()); //$NON-NLS-1$ //$NON-NLS-2$
		
		this.setCurrentState(Messages.getString("NoteCopySlave.storing"), task, noteSize); //$NON-NLS-1$
		Note dstNote = target.createNote(srcNote, task.getNotebookName());
		if (dstNote != null){
			source.addNewTag(task.getNote().getGuid(), sourceTag);
			target.addNewTag(dstNote.getGuid(), targetTag);
			copyState.addCopied();
		} else {
			copyState.subtractUploadedSize(noteSize);
			copyState.addFailed();
		}
	}

	public synchronized String getCurrentState(){
		return currentState;
	}
	
	private synchronized void setCurrentState(String action, NoteCopyTask task, long noteSize){
		if (task ==null){
			this.currentState = "["+action+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			String notebookName = task.getNotebookName();
			if (notebookName.length()>this.stateMaxNotebookName){
				notebookName = task.getNotebookName().substring(0,this.stateMaxNotebookName-3)+"..."; //$NON-NLS-1$
			}
			String noteTitle = task.getNote().getTitle();
			if (noteTitle.length()>this.stateMaxNoteTitle){
				noteTitle = task.getNote().getTitle().substring(0, stateMaxNoteTitle-3)+"..."; //$NON-NLS-1$
			}
			this.currentState = String.format(this.stateFormat, 
					action, 
					notebookName,
					task.getCurrentNotebookNumber(),
					task.getTotalNotebookNumber(),
					noteTitle,
					task.getCurrentNoteNumberInCurrentNotebook(),
					task.getTotalNoteNumberInCurrentNotebook(),
					noteSize
					);
		}
	}
	public boolean isDone(){
		return this.done;
	}
	public void setLastTask(NoteCopyTask task){
		this.lastTask = task;
	}
	public NoteCopyTask getLastTask(){
		final NoteCopyTask ret = this.lastTask;
		this.lastTask = null;
		return ret;
	}
}
