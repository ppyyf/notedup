package com.evernote.trunk.nodedup;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.evernote.thrift.TException;
import com.evernote.thrift.protocol.TBinaryProtocol;
import com.evernote.thrift.transport.THttpClient;
import com.evernote.thrift.transport.TTransportException;

import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.edam.type.SavedSearch;
import com.evernote.edam.type.Tag;
import com.evernote.edam.userstore.Constants;
import com.evernote.edam.userstore.UserStore;

public class Account {
	private String evernoteHost;
	private String authToken;
	private boolean initialized = false;
	private String username;
	private int userId;
	private String shardId;

	private UserStore.Client userStore;
	private NoteStore.Client noteStore;
	private HashMap<String,Notebook> notebookGuids = new HashMap<String, Notebook>();
	private HashMap<String,Notebook> notebookNames = new HashMap<String, Notebook>();
	private Notebook defaultNotebook;
	private HashMap<String,Tag> tagGuids = new HashMap<String, Tag>();
	private HashMap<String,Tag> tagNames = new HashMap<String, Tag>();
	private final int retryInterval = 5000;
	private final int retry = 12;
	
	public int getUserId() {
		return userId;
	}

	public String getShardId() {
		return shardId;
	}

	public String getEvernoteHost() {
		return evernoteHost;
	}

	public String getUsername() {
		return username;
	}

	public Account(String evernoteHost,String authToken){
		this.evernoteHost = evernoteHost;
		this.authToken = authToken;
		this.initialize();
	}
	
	private void initialize(){
		if (this.evernoteHost != null && this.authToken != null){
			String userStoreUrl = "https://" + this.evernoteHost + "/edam/user"; //$NON-NLS-1$ //$NON-NLS-2$
			String userAgent = "NoteDup " + //$NON-NLS-1$
					Constants.EDAM_VERSION_MAJOR + "." + //$NON-NLS-1$
					Constants.EDAM_VERSION_MINOR;
			try {
				THttpClient userStoreTrans = new THttpClient(userStoreUrl);
				userStoreTrans.setCustomHeader("User-Agent", userAgent); //$NON-NLS-1$
				TBinaryProtocol userStoreProt = new TBinaryProtocol(userStoreTrans);
				this.userStore = new UserStore.Client(userStoreProt, userStoreProt);
				boolean versionOk = this.userStore.checkVersion("NoteDup ", //$NON-NLS-1$
						com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
						com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
				if (!versionOk) {
					System.err.println("Incomatible Evernote client protocol version"); //$NON-NLS-1$
				} else {
					String notestoreUrl = this.userStore.getNoteStoreUrl(authToken);
					THttpClient noteStoreTrans = new THttpClient(notestoreUrl);
					noteStoreTrans.setCustomHeader("User-Agent", userAgent); //$NON-NLS-1$
					TBinaryProtocol noteStoreProt = new TBinaryProtocol(noteStoreTrans);
					this.noteStore = new NoteStore.Client(noteStoreProt, noteStoreProt);
					this.username = this.userStore.getUser(authToken).getUsername();
					this.userId = this.userStore.getUser(authToken).getId();
					this.shardId = this.userStore.getUser(authToken).getShardId();
					this.initialized = true;
					this.updateNotebookHashes();
					this.updateTagHashes();
				}
			} catch (TTransportException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void updateNotebookHashes(){
		if (this.initialized){
			List<Notebook> books;
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					books = this.noteStore.listNotebooks(this.authToken);
					if (books != null){
						this.notebookGuids.clear();
						this.notebookNames.clear();
						this.defaultNotebook = null;
						for (Notebook notebook : books) {
							this.notebookNames.put(notebook.getName().toLowerCase(), notebook);
							this.notebookGuids.put(notebook.getGuid(), notebook);
							if (notebook.isDefaultNotebook()){
								this.defaultNotebook = notebook;
							}
						}
					}
					break;
				} catch (EDAMUserException e) {
				} catch (EDAMSystemException e) {
				} catch (TException e) {
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	
	public boolean isInitialized(){
		return this.initialized;
	}
	
	public List<Notebook> getNotebooks(){
		List<Notebook> ret = null;
		if (this.isInitialized()){
			ret = new LinkedList<Notebook>();
			ret.addAll(this.notebookNames.values());
			// put the default notebook first.
			ret.remove(this.defaultNotebook);
			ret.add(0, this.defaultNotebook);
		}
		return ret;
	}

	public Notebook getNotebookByName(String name){
		return this.notebookNames.get(name.toLowerCase());
	}
	
	public Notebook getNotebookByGuid(String guid){
		return this.notebookGuids.get(guid);
	}
	
	public Notebook getDefaultNotebook(){
		return this.defaultNotebook;
	}
	
	public List<Tag> getTags(){
		List<Tag> ret = null;
		if(this.isInitialized()){
			ret = new LinkedList<Tag>();
			ret.addAll(this.tagNames.values());
		}
		return ret;
	}
	
	public Tag getTagByName(String name){
		return this.tagNames.get(name.toLowerCase());
	}
	
	public Tag getTagByGuid(String guid){
		return this.tagGuids.get(guid);
	}

	public List<Note> getNotesInNotebook(String notebookGuid){
		List<Note> ret = null;
		if (notebookGuid != null){
			NoteFilter filter = new NoteFilter();
			filter.setNotebookGuid(notebookGuid);
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					NoteList nl = noteStore.findNotes(authToken, filter, 0, 50);
					ret = nl.getNotes();
					while(ret.size()<nl.getTotalNotes()){
						nl = noteStore.findNotes(authToken, filter, ret.size(), 50);
						ret.addAll(nl.getNotes());
					}
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		}
		return ret;
	}
	
	public Note getNote(String noteGuid){
		Note ret = null;
		if (noteGuid != null){
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					ret = noteStore.getNote(authToken, noteGuid, true, true, true, true);
					ret.getAttributes().setApplicationData(noteStore.getNoteApplicationData(authToken, noteGuid));
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		}
		return ret;
	}
	
	public String createNotebook(String name, String stackName){
		String ret = name;
		Notebook book = this.getNotebookByName(name);
		if (stackName != null && stackName.equals("")){ //$NON-NLS-1$
			stackName = null;
		}
		if(book != null){
			if ((book.getStack() == null && stackName == null) || (book.getStack() !=null && book.getStack().equals(stackName))){
				ret = name;
			} else {
				ret = null;
			}
		} else {
			ret = null;
			book = new Notebook();
			book.setName(name);
			book.setStack(stackName);
			Notebook newbook;
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					newbook = this.noteStore.createNotebook(this.authToken, book);
					this.notebookGuids.put(newbook.getGuid(), newbook);
					this.notebookNames.put(newbook.getName().toLowerCase(), newbook);
					ret = newbook.getName();
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		}
		return ret;
	}
		
	// Save note in notebook or the default notebook if notebookName is ""
	public Note createNote(Note note, String notebookName){
		Note ret = null;
		if(notebookName == null || notebookName.equals("")){ //$NON-NLS-1$
			notebookName = this.getDefaultNotebook().getName();
		}
		if (this.getNotebookByName(notebookName)==null){
			this.updateNotebookHashes();
		}
		if (this.getNotebookByName(notebookName)!=null){
			note.setNotebookGuid(this.getNotebookByName(notebookName).getGuid());
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					ret = this.noteStore.createNote(this.authToken, note);
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		}
		return ret;
	}
	
	public boolean addNewTag(String noteGuid, String tag){
		boolean ret = false;
		List<String> tagList = new LinkedList<String>();
		tagList.add(tag);
		int tried = 0;
		while(tried<this.retry){
			tried ++;
			try {
				Note note = this.noteStore.getNote(this.authToken, noteGuid, false, false, false, false);
				note.setTagNames(tagList);
				this.noteStore.updateNote(this.authToken, note);
				ret = true;
				break;
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			} catch (EDAMNotFoundException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			}
		}
		return ret;
	}
	public boolean hasTag(Note note, String tag){
		boolean ret = false;
		if (this.tagNames.containsKey(tag.toLowerCase()) && note.getTagGuids() != null && note.getTagGuids().contains(this.tagNames.get(tag.toLowerCase()).getGuid())){
			ret = true;
		}
		return ret;
	}
	public Tag getTag(String guid){
		Tag ret = null;
		ret = this.getTagByGuid(guid);
		if (ret !=null){
			return ret;
		}
		int tried = 0;
		while(tried<this.retry){
			tried ++;
			try {
				ret = this.noteStore.getTag(this.authToken, guid);
				break;
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			} catch (EDAMNotFoundException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			}
		}
		return ret;
	}
	
	public void updateTagHashes(){
		if (this.initialized){
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					this.tagGuids.clear();
					this.tagNames.clear();
					List<Tag> tags = this.noteStore.listTags(this.authToken);
					for (Tag tag : tags){
						this.tagGuids.put(tag.getGuid(), tag);
						this.tagNames.put(tag.getName().toLowerCase(), tag);
					}
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		}
	}
	public boolean createTag(String name, String parentName){
		boolean ret = false;
		this.updateTagHashes();
		String parentGuid = null;
		if (parentName != null){
			if (this.getTagByName(parentName)==null){
				Tag tag = new Tag();
				tag.setName(parentName);
				int tried = 0;
				while(tried<this.retry){
					tried ++;
					try {
						Tag newTag = this.noteStore.createTag(this.authToken, tag);
						parentGuid = newTag.getGuid();
						this.updateTagHashes();
						break;
					} catch (EDAMUserException e) {
						e.printStackTrace();
					} catch (EDAMSystemException e) {
						e.printStackTrace();
					} catch (EDAMNotFoundException e) {
						e.printStackTrace();
					} catch (TException e) {
						e.printStackTrace();
					}
					try {
						Thread.sleep(retryInterval);
					} catch (InterruptedException e) {
					}
				}
			} else {
				parentGuid = this.getTagByName(parentName).getGuid();
			}
		}
		
		if(this.getTagByName(name)==null){
			Tag tag = new Tag();
			tag.setName(name);
			if(parentGuid != null){
				tag.setParentGuid(parentGuid);
			}
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					this.noteStore.createTag(this.authToken, tag);
					this.updateTagHashes();
					ret = true;
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		} else if (this.getTagByName(name).getParentGuid() == null && parentGuid != null) {
			Tag tag = this.getTagByName(name);
			tag.setParentGuid(parentGuid);
			int tried = 0;
			while(tried<this.retry){
				tried ++;
				try {
					this.noteStore.updateTag(this.authToken, tag);
					ret = true;
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
		} else {
			ret = true;
		}
		
		return ret;
	}
	
	public long getUploadLimit(){
		long uploadLimit = 0;
		int tried = 0;
		while(tried<this.retry){
			tried ++;
			try {
				uploadLimit = this.userStore.getUser(this.authToken).getAccounting().getUploadLimit() -this.noteStore.getSyncState(this.authToken).getUploaded();
				break;
			} catch (EDAMUserException e) {
			} catch (EDAMSystemException e) {
			} catch (TException e) {
			}
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			}
		}
		return uploadLimit;
	}

	public void setNoteApplicationDataEntry(String guid, String value){
		int tried = 0;
		while(tried<this.retry){
			tried ++;
			try {
				this.noteStore.setNoteApplicationDataEntry(this.authToken, guid, AuthHelperSystemBrowser.getKey(), value);
				break;
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			}
		}
	}	
	
	public static long noteSize(Note note){
		long size=0;
		size += note.getContentLength();
		if (note.getResources() !=null){
			for (Resource resource : note.getResources()){
				size+=resource.getData().getSize();
			}
		}
		return size;
	}
	
	@Override
	public Account clone(){
		return new Account(this.evernoteHost,this.authToken);
	}
	
	public int updateNoteLinks(Account source){
		List<NoteMetadata> copiedNotes = null;
		NoteFilter filter = new NoteFilter();
		filter.setWords("applicationData:"+AuthHelperSystemBrowser.getKey()); //$NON-NLS-1$
		NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
		int tried = 0;
		// get all notes created by NoteDup
		while(tried<this.retry){
			tried ++;
			try {
				NotesMetadataList nml = noteStore.findNotesMetadata(authToken, filter, 0, 50, spec);
				copiedNotes = nml.getNotes();
				while(copiedNotes.size()<nml.getTotalNotes()){
					nml = noteStore.findNotesMetadata(authToken, filter, copiedNotes.size(), 50,spec);
					copiedNotes.addAll(nml.getNotes());
				}
				break;
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			} catch (EDAMNotFoundException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			}
		}
		
		//build map of original/new guids
		Map<String,String> guidMap = new HashMap<String,String>();
		for (NoteMetadata note : copiedNotes){
			int tried2 = 0;
			while(tried2 < this.retry){
				tried2 ++;
				try {
					String value = this.noteStore.getNoteApplicationDataEntry(this.authToken, note.getGuid(), AuthHelperSystemBrowser.getKey());
					String[] values = value.split("/"); //$NON-NLS-1$
					if (values[0]!=null && values[0].equals(String.valueOf(source.getUserId())) &&
							values[1]!=null && values[1].equals(source.getShardId()) && 
							values[2]!=null ){
						guidMap.put(values[2], note.getGuid());
					}
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
			}
		}
		
		return replaceNoteLinks(source, copiedNotes, guidMap);
	}

	private int replaceNoteLinks(Account origAccount,
			List<NoteMetadata> copiedNotes, Map<String, String> guidMap) {
		// find and replace evernote:///view/[src_uid]/[src_sid]/[old_guid]/[old_guid]/ to 
		// evernote:///view/[uid]/[sid]/[new_guid]/[new_guid]/
		String notelinkprefix = "evernote:///view/"+origAccount.getUserId()+"/"+origAccount.getShardId()+"/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String newprefix = "evernote:///view/"+this.getUserId()+"/"+this.getShardId()+"/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Pattern PATTERN_GUID = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"); //$NON-NLS-1$
		int count = 0;
		for (NoteMetadata note : copiedNotes){
			int tried2 = 0;
			while(tried2 < this.retry){
				tried2 ++;
				try {
					Note notecontent = this.noteStore.getNote(this.authToken, note.getGuid(),true,false,false,false);
					String content = notecontent.getContent();
					
					int pos = content.indexOf(notelinkprefix, 0);
					if (pos >= 0){
						boolean updated = false;
						StringBuffer sb = new StringBuffer(content);
						// replacing
						while (pos >= 0){
							String guid1 = sb.substring(pos+notelinkprefix.length(), pos+notelinkprefix.length()+36);
							String guid2 = sb.substring(pos+notelinkprefix.length()+37, pos+notelinkprefix.length()+73);
							if (guid1.equals(guid2) && PATTERN_GUID.matcher(guid1).matches() && guidMap.containsKey(guid1)){
								sb.replace(pos+notelinkprefix.length(), pos+notelinkprefix.length()+36, guidMap.get(guid1));
								sb.replace(pos+notelinkprefix.length()+37, pos+notelinkprefix.length()+73, guidMap.get(guid1));
								sb.replace(pos, pos+notelinkprefix.length(), newprefix);
								updated = true;
							}
							pos = sb.indexOf(notelinkprefix,pos+1);
						}
						if (updated){
							notecontent.setContent(sb.toString());
							Note updatedNote = this.noteStore.updateNote(this.authToken, notecontent);
							if (updatedNote !=null && updatedNote.getGuid().equals(notecontent.getGuid())){
								count ++;
							}
						}
					}
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
			}
		}
		return count;
	}
	
	public int removeInvalidSharedLinks(){
		if (!this.evernoteHost.contains("yinxiang")){ //$NON-NLS-1$
			return 0;
		}
		int count = 0;
		Map<String, String> guidMap = new HashMap<String,String>();
		List<NoteMetadata> sharedNotes = null;
		NoteFilter filter = new NoteFilter();
		filter.setWords("shareDate:*"); //$NON-NLS-1$
		NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
		spec.setIncludeAttributes(true);
		int tried = 0;
		// get all notes with shareDate attribute
		while(tried<this.retry){
			tried ++;
			try {
				NotesMetadataList nml = noteStore.findNotesMetadata(authToken, filter, 0, 50, spec);
				sharedNotes = nml.getNotes();
				while(sharedNotes.size() < nml.getTotalNotes()){
					nml = noteStore.findNotesMetadata(authToken, filter, sharedNotes.size(), 50,spec);
					sharedNotes.addAll(nml.getNotes());
				}
				break;
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			} catch (EDAMNotFoundException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(retryInterval);
			} catch (InterruptedException e) {
			}
		}		
		
		for (NoteMetadata noteMetadata : sharedNotes){
			int tried2 = 0;
			while(tried2 < this.retry){
				tried2 ++;
				try {
					Note srcNote = this.getNote(noteMetadata.getGuid());
					srcNote.getAttributes().unsetShareDate();
					Note dstNote = this.createNote(srcNote, this.getNotebookByGuid(srcNote.getNotebookGuid()).getName());
					if (dstNote !=null){
						try {
							this.noteStore.deleteNote(this.authToken, srcNote.getGuid());
						} catch (Exception e) {
							e.printStackTrace();
						}
						guidMap.put(srcNote.getGuid(), dstNote.getGuid());
						count++;
					}
					break;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
		if (guidMap.size() > 0){
			List<NoteMetadata> allNotes = null;
			NoteFilter filter1 = new NoteFilter();
			NotesMetadataResultSpec spec1 = new NotesMetadataResultSpec();
			int tried2 = 0;
			while(tried2<this.retry){
				tried2 ++;
				try {
					NotesMetadataList nml = noteStore.findNotesMetadata(authToken, filter1, 0, 50, spec1);
					allNotes = nml.getNotes();
					while(allNotes.size()<nml.getTotalNotes()){
						nml = noteStore.findNotesMetadata(authToken, filter1, allNotes.size(), 50,spec1);
						allNotes.addAll(nml.getNotes());
					}
					break;
				} catch (EDAMUserException e) {
					e.printStackTrace();
				} catch (EDAMSystemException e) {
					e.printStackTrace();
				} catch (EDAMNotFoundException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
				try {
					Thread.sleep(retryInterval);
				} catch (InterruptedException e) {
				}
			}
			this.replaceNoteLinks(this, allNotes, guidMap);
		}
		return count;
	}

	public List<SavedSearch> getSavedSearches(){
		List<SavedSearch> res = null;
		int tried = 0;
		while(tried<this.retry){
			tried ++;
			try {
				res = this.noteStore.listSearches(authToken);
				break;
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	public boolean createSavedSearch(SavedSearch s){
		boolean res = false;
		int tried = 0;
		while(tried<this.retry){
			tried ++;
			try {
				SavedSearch ret = this.noteStore.createSearch(authToken, s);
				if (ret != null && ret.getGuid() != null){
					res = true;
				}
				break;
			} catch (EDAMUserException e) {
				e.printStackTrace();
			} catch (EDAMSystemException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
		}
		return res;
	}
}
