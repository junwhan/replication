package aleph.dir;

import org.deuce.transaction.TransactionException;

public class NotRegisteredKeyException extends TransactionException{
	private Object key;
	public NotRegisteredKeyException(Object key) {
		this.key = key;
	}
	
	@Override
	public String getMessage() {
		return "Key Not Found in local Registery [" + key + "]";
	}
}
