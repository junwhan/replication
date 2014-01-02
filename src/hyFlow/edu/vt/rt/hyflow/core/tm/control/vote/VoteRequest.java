package edu.vt.rt.hyflow.core.tm.control.vote;

import aleph.AsynchMessage;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.util.io.Logger;

public class VoteRequest extends AsynchMessage{
	private Long txnId;
	public VoteRequest(Long txnId){
		this.txnId = txnId;
	}
	
	@Override
	public void run() {
		Logger.debug(txnId + ": Request from " + from);
		ControlContext.vote(txnId, from);
	}

}
