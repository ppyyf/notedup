package com.evernote.trunk.nodedup;

public class CopyState {
	private int copied = 0;
	private int failed = 0;
	private int skipped = 0;
	private int notcopied = 0;
	private int nbcopied = 0;
	private long uploadedSize = 0;
	private long queueSize = 0;
	private boolean done = false;
	private boolean stopped = false;
	private final long maxUploadSize;
	
	public CopyState(long maxUploadSize){
		this.maxUploadSize = maxUploadSize;
	}
	
	public int getCopied() {
		return copied;
	}
	public int getFailed() {
		return failed;
	}
	public int getSkipped() {
		return skipped;
	}
	public int getNotcopied() {
		return notcopied;
	}
	public int getNbcopied() {
		return nbcopied;
	}
	public synchronized long getUploadedSize() {
		return uploadedSize;
	}
	public synchronized long getQueueSize() {
		return queueSize;
	}
	
	public boolean isDone(){
		return this.done;
	}
	public boolean isStopped(){
		return this.stopped;
	}
	public synchronized void addCopied() {
		this.copied++;
	}
	public synchronized void addFailed() {
		this.failed++;
	}
	public synchronized void addSkipped() {
		this.skipped++;
	}
	public synchronized void addNotcopied() {
		this.notcopied++;
	}
	public synchronized void addNbcopied() {
		this.nbcopied++;
	}
	public synchronized boolean testAndAddUploadedSize(long uploadedSize) {
		if (this.uploadedSize+uploadedSize> this.maxUploadSize){
			return false;
		}
		this.uploadedSize += uploadedSize;
		return true;
	}
	public synchronized long queueCapacity(){
		return this.maxUploadSize-this.uploadedSize-this.queueSize;
	}
	public synchronized boolean testUploadedSize(long uploadedSize) {
		if (this.uploadedSize+uploadedSize > this.maxUploadSize){
			return false;
		} else {
			return true;
		}
	}
	public synchronized void addQueueSize(long size){
		this.queueSize += size;
	}
	public synchronized void subtractQueueSize(long size){
		this.queueSize -= size;
	}
	
	public synchronized void subtractUploadedSize(long uploadedSize) {
		this.uploadedSize -= uploadedSize;
	}
	public synchronized void done(){
		this.done = true;
	}
	public synchronized void stop(){
		this.stopped = true;
	}
}
