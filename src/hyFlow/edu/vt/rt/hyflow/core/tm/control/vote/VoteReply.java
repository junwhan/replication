package edu.vt.rt.hyflow.core.tm.control.vote;

import aleph.AsynchMessage;
import edu.vt.rt.hyflow.core.tm.control.ControlContext;
import edu.vt.rt.hyflow.util.io.Logger;

public class VoteReply extends AsynchMessage{
	private Long txnId;
	private Boolean decision;
	public VoteReply(Long txnId, Boolean decision){
		this.txnId = txnId;
		this.decision = decision;
	}
	
	@Override
	public void run() {
		Logger.debug(txnId + ": Reply from " + from);
		ControlContext.voteReply(txnId, from, decision);
	}

}
