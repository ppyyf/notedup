package com.evernote.trunk.nodedup;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;

public class NoteCopyMaster extends Thread {
	private final Account source;
	private final Account target;
	private final String sourceTag;
	private final CopyState copyState;
	private final BlockingQueue<NoteCopyTask> noteQueue;
	private final List<String> selectedNotebooks;
	private final List<String> selectedTags;
	private int currentNotebookNum;
	private int totalNotebookNum;
	private String currentState;

	public NoteCopyMaster(Account source, Account target, final String sourceTag, final String targetTag, BlockingQueue<NoteCopyTask> noteQueue, CopyState copyState, List<String> selectedNotebooks, List<String> selectedTags){
		this.source = source;
		this.target = target;
		this.sourceTag = sourceTag;
		this.copyState = copyState;
		this.noteQueue = noteQueue;
		this.selectedNotebooks = selectedNotebooks;
		this.selectedTags = selectedTags;
		this.currentNotebookNum = 0;
		this.totalNotebookNum = source.getNotebooks().size();
		currentState = "["+Messages.getString("NoteCopyMaster.state.init")+"]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	private synchronized void setCurrentState(String state, String objtype, String objname){
		currentState = "["+state+"]"; //$NON-NLS-1$ //$NON-NLS-2$
		if (objtype!=null){
			currentState += " "+ objtype; //$NON-NLS-1$
		}
		if (objname!=null){
			currentState += ": "+ objname; //$NON-NLS-1$
		}
	}
	
	@Override
	public void run() {
		for(Notebook notebook:source.getNotebooks()){
			if (copyState.isStopped()){
				break;
			}
			this.currentNotebookNum ++;
			
			if (selectedNotebooks!=null && !selectedNotebooks.contains(notebook.getName())){
				continue;
			}
			copyState.addNbcopied();
			String dstNotebookName = notebook.getName();
			setCurrentState(Messages.getString("NoteCopyMaster.state.checking"), Messages.getString("NoteCopyMaster.state.notebook"), notebook.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			int ncount = source.getNotesInNotebook(notebook.getGuid()).size();
			// make sure the notebook is present in target account.
			if(ncount > 0 && target.getNotebookByName(notebook.getName()) == null){
				setCurrentState(Messages.getString("NoteCopyMaster.state.creating"), Messages.getString("NoteCopyMaster.state.notebook"), notebook.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				target.createNotebook(notebook.getName(), notebook.getStack());
			}
			int ncurrent = 0;
			for(Note note:source.getNotesInNotebook(notebook.getGuid())){
				if (copyState.isStopped()){
					break;
				}
				ncurrent++;
				
				setCurrentState(Messages.getString("NoteCopyMaster.state.checking"), Messages.getString("NoteCopyMaster.state.note"), note.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$
				// tag not selected
				if (selectedTags !=null){
					if (note.getTagGuids()==null || note.getTagGuids().size()==0){
						if (!selectedTags.contains(SelectionFrame.noTagsCheckBoxText)){
							continue;
						}
					} else {
						boolean selected = false;
						boolean allNDTags = true;
						for (String guid : note.getTagGuids()){
							if (!source.getTagByGuid(guid).getName().startsWith("__ND_")){ //$NON-NLS-1$
								allNDTags = false;
							}
							if (selectedTags.contains(source.getTagByGuid(guid).getName())){
								selected = true;
							}
						}
						if (allNDTags&&!selectedTags.contains(SelectionFrame.noTagsCheckBoxText)){
							continue;
						} else if (!selected && !allNDTags){
							continue;
						}
					}
				}
				
				// already copied.
				if (source.hasTag(note, sourceTag)){
					copyState.addSkipped();
					continue;
				}
				
				// too large
				long noteSize = Account.noteSize(note); 
				if ( noteSize > copyState.queueCapacity()){
					copyState.addNotcopied();
					continue;
				}
				
				// enqueue
				setCurrentState(Messages.getString("NoteCopyMaster.state.enquequing"), Messages.getString("NoteCopyMaster.state.note"), note.getTitle()); //$NON-NLS-1$ //$NON-NLS-2$
				while(true){
					try {
						noteQueue.put(new NoteCopyTask(note, dstNotebookName, ncount, ncurrent,this.totalNotebookNum,this.currentNotebookNum));
						copyState.addQueueSize(noteSize);
						break;
					} catch (InterruptedException e) {
						if (copyState.isStopped()){
							break;
						}
					}
				}
			}
		}
		// enqueue one ending task with null note object
		setCurrentState(Messages.getString("NoteCopyMaster.state.done"), null, null); //$NON-NLS-1$
		if (!copyState.isStopped()){
			while(true){
				try {
					noteQueue.put(new NoteCopyTask(null, null, 0, 0, 0, 0));
					break;
				} catch (InterruptedException e) {
					if (copyState.isStopped()){
						break;
					}
				}
			}
		}
		// no more notes to enqueue
		copyState.done();
	}
	public synchronized String getCurrentState(){
		return currentState;
	}
}
