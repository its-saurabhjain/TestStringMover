package com.xerox.TestStringMover;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TimerTask;

import com.xerox.util.io.FileParser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TestScanForWork extends TimerTask {

	private static Log log = LogFactory.getFactory().getInstance(TestScanForWork.class);
	private static MQMessagingService mqms = null;
	private static Hashtable<String,String> fileList = new Hashtable<String,String>();

	public TestScanForWork() {
		super();
		if( log.isDebugEnabled() ) {
			log.debug("ScanForWork()");
		}
		mqms = new MQMessagingService();
		
		if( mqms == null ) {
			if( log.isErrorEnabled() ) {
				log.error("Failed to initialize MQ Messaging Service");
			}
		}
		try {
			if( log.isDebugEnabled() ) {
				log.debug("constructing: " + Configurator.getInstance().getMQ_QUEUE_MANAGER());
				log.debug("constructing: " + Configurator.getInstance().getMQ_UPLOAD_QUEUE());
				log.debug("constructing: " + Configurator.getInstance().getMQ_DOWNLOAD_QUEUE());
				log.debug("constructing: " + Configurator.getInstance().getMQ_CHANNEL());
				log.debug("constructing: " + Configurator.getInstance().getMQ_HOST());
				log.debug("constructing: " + Configurator.getInstance().getMQ_PORT());
				log.debug("constructing: " + Configurator.getInstance().getMQ_USERID());
			}
			mqms.setQueueManagerName(Configurator.getInstance().getMQ_QUEUE_MANAGER());
			mqms.setMQChannel(Configurator.getInstance().getMQ_CHANNEL());
			mqms.setMQHost(Configurator.getInstance().getMQ_HOST());
			mqms.setMQPort(Configurator.getInstance().getMQ_PORT());
			mqms.setMQUserid(Configurator.getInstance().getMQ_USERID());
		} catch (Exception e) {
			if( log.isErrorEnabled() ) {
				log.error("Failed to configure MQ Messaging Service");
			}
		}
	}
	public void run() {
		if( log.isDebugEnabled() ) {
			log.debug("ScanForWork.run()");
		}
		boolean fileMoved = false;
		boolean messageSent = false;
		String messageReceived="";
		// check directory for files with the desired extensions
		File getdir = new File(Configurator.getInstance().getWATCH_DIRECTORY());
		File newloc = new File(Configurator.getInstance().getSEND_DIRECTORY());
		File invalid = new File(Configurator.getInstance().getINVLAID_DIRECTORY());
		try
		{
			///
			Collection files = DirectoryScanner.getFilesInDirectoryFIFO(getdir, Configurator.getInstance().getFILE_EXTENSIONS());
			int settleTime= Configurator.getInstance().getFILE_SETTLE_PERIOD();
			log.debug("Sleeping for " + settleTime + " seconds to let files settle before acting upon them...");
			Thread.sleep(1000L * settleTime);
			// keep trying until some file succeeds or no files are left to try 
			Iterator it = files.iterator();
			while(it.hasNext()) {
				// grab the next one
				File file = (File) it.next();
				String name = file.getName();
				// it is necessary to capture the file size here, before it is moved
				String filelen = Long.toString(file.length());
				if( log.isDebugEnabled() ) {
					log.debug("file length = " + file.length());
				}
				//Check if the message is in the process list or not and if not add it to the list, so that it will not be processed again in case of failure
				FileParser fileParser= new FileParser();
				//Parse File and create a Ping MQ Message
				String pingMessage = fileParser.parseFile(file.getAbsolutePath());
				String msgCor = pingMessage.substring(0, 25);
				String hasFile = fileList.get(msgCor);
				//send Ping MQ message if it has not been sent
				if(hasFile == null)
				{
					messageSent = sendMessage(pingMessage);
					fileList.put(msgCor, pingMessage);
				}
				String fileMessage = fileParser.parseFile2(file.getAbsolutePath());
				messageSent = sendMessage(fileMessage);
				fileParser.moveFile(file, newloc, invalid);
			}
			//Send Response messages to the download queue
			//messageSent = sendMessage(pingMessage.substring(0, 25)+ "RC=00");
			String message= receiveMessage();
		} //Try block ends here
		catch(Exception exp)
		{
			if( log.isErrorEnabled() ) {
				log.error("Error while polling the source folder :"+exp.getMessage());
			}
		}
	}
	
	private boolean sendMessage(String mqMessage) {
		boolean success = true;
		try {
				mqms.setQueueName(Configurator.getInstance().getMQ_UPLOAD_QUEUE());
				//mqms.setQueueName(Configurator.getInstance().getMQ_DOWNLOAD_QUEUE());
				mqms.send(mqMessage);
			
		} catch (Exception e) {
			success = false;
			if( log.isErrorEnabled() ) {
				log.error("Failed to send MQ message for " + mqMessage, e);
			}
		}
		return success;
	}
	private String receiveMessage()
	{
		String messageReceived = "";
		String sendFile = "";
		boolean isMessageReceived = false;
		try{
			mqms.setQueueName(Configurator.getInstance().getMQ_UPLOAD_QUEUE());
			//mqms.setQueueName(Configurator.getInstance().getMQ_DOWNLOAD_QUEUE());	
			messageReceived = mqms.receive();
			if(messageReceived == null || messageReceived == "")
				messageReceived = "0112016/05/23091215501130RC=00";
		}catch (Exception e){
			if( log.isErrorEnabled() ) {
				log.error("Failed to receive MQ message", e);
			}
			
		}
		return messageReceived;
	}
}
