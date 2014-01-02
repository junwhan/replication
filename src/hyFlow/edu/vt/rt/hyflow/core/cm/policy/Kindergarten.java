package edu.vt.rt.hyflow.core.cm.policy;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.deuce.transaction.AbstractContext;
import org.deuce.transaction.TransactionException;

import edu.vt.rt.hyflow.util.network.Network;

public class Kindergarten extends AbstractContentionPolicy{

	private static final Integer MAX_RETRIES = 4;
	private static final Integer BACKOFF = 10;
	private static long counter = Network.getInstance().getID() * 100;

	@Override
	public void init(AbstractContext context) {
		if(context.contentionMetadata==null){
			System.out.println("Init " + context);
			context.contentionMetadata = new Object[]{
				new LinkedList<Long>(),
				0,
				null,
				new Date().getTime(),
				counter++,
			};
		}
	}
	
	@Override
	public void complete(AbstractContext context) {
		System.out.println("Complete ------------------------------------- " + context);
		context.contentionMetadata = null;
	}
	
	@Override
	public int resolve(AbstractContext context1, AbstractContext context2) {
		if(super.resolve(context1, context2)==0)
			return 0;
		
		Object[] metaData1 = (Object[])context1.contentionMetadata;
		Object[] metaData2 = (Object[])context2.contentionMetadata;
		
		System.out.println(metaData1[4] + " vs " + metaData2[4]);
		
		List<Long> enemies1 =(List<Long>)metaData1[0];
		List<Long> enemies2 =(List<Long>)metaData1[0];
		System.out.println(metaData1[4] + " Enemies:" + Arrays.toString(enemies1.toArray()));
		System.out.println(metaData2[4] + " Enemies:" + Arrays.toString(enemies2.toArray()));
		
		if(enemies1.contains(metaData2[4])){
			if(enemies2.contains(metaData1[4])){	// tie breaker
				Long timestamp1 =(Long)metaData1[3];
				Long timestamp2 =(Long)metaData2[3];
				if(timestamp1<timestamp2){
					System.out.println("Tie Break" + timestamp1 + " <" + timestamp2);
					try {
						Thread.sleep(BACKOFF);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					throw new TransactionException();
				}
			}
			System.out.println("Old Enemy 1:" + metaData2[4]);
			context2.rollback();
			return 0;
		}
			
		Integer retries1 =(Integer)metaData1[1];
		if(retries1>MAX_RETRIES){
			System.out.println(metaData2[4] + " enemy of " + metaData1[4]);
			enemies1.add((Long)metaData2[4]);	// add context2 as enemy
			metaData1[1] = 0;
			try {
				Thread.sleep(BACKOFF);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			throw new TransactionException();
		}
		
		if(metaData2[4].equals(metaData1[2]))
			metaData1[1] = ++retries1;
		else
			metaData1[1] = 0;
		
		metaData1[2] = metaData2[4];
		
		System.out.println(metaData1[4] + " Wait " + retries1);
		return BACKOFF;
	}
}
