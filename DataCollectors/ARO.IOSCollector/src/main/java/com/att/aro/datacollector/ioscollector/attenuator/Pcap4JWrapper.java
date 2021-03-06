package com.att.aro.datacollector.ioscollector.attenuator;

import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.littleshoot.proxy.TransportProtocol;
import org.pcap4j.core.BpfProgram.BpfCompileMode;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapDumper;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.core.Pcaps;

import com.att.aro.core.util.Util;

public class Pcap4JWrapper implements Runnable{
	private static final Logger LOG = LogManager.getLogger(Pcap4JWrapper.class);
	private final String COUNT_KEY = Pcap4JWrapper.class.getName() + ".count";
	private final int COUNT = Integer.getInteger(COUNT_KEY, -1);
	private final String READ_TIMEOUT_KEY = Pcap4JWrapper.class.getName() + ".readTimeout";
	private final int READ_TIMEOUT = Integer.getInteger(READ_TIMEOUT_KEY, 10); // [ms]
	private final String SNAPLEN_KEY = Pcap4JWrapper.class.getName() + ".snaplen";
	private final int SNAPLEN = Integer.getInteger(SNAPLEN_KEY, 65536); // [bytes]
	private final String PCAP_FILE_KEY = Pcap4JWrapper.class.getName() + ".pcapFile";
	private final String PCAP_FILE = System.getProperty(PCAP_FILE_KEY, "DumpLoop.pcap");
	private String PCAP_FILE_PATH = "";
	private PcapDumper dumper;
	private 	PcapHandle handle;

	protected final ConcurrentSkipListSet<TransportProtocol> TRANSPORTS_USED = new ConcurrentSkipListSet<TransportProtocol>();

	@Override
	public void run() {
		getPcapFile(PCAP_FILE_PATH) ;
	}
 
	private void getPcapFile(String trafficFilePath) {

		PcapNetworkInterface nif;
		try {
			nif = Pcaps.getDevByName("bridge100");		
		} catch (/*IOException | */PcapNativeException pcape) {
			LOG.info("PcapNativeException :", pcape);
			return;
		} 
		
		if (nif == null) {
			return;
		}

		LOG.info(nif.getName() + "(" + nif.getDescription() + ")");

		try {
		    handle
		      = nif.openLive(SNAPLEN, PromiscuousMode.PROMISCUOUS, READ_TIMEOUT);
		    handle.setFilter("", BpfCompileMode.OPTIMIZE);

		    dumper = handle.dumpOpen(trafficFilePath+ Util.FILE_SEPARATOR + PCAP_FILE);
			try {
			      handle.loop(COUNT, dumper);
		    } catch (InterruptedException e) {
				LOG.error("InterruptedException :", e);
		    }
		} catch (PcapNativeException | NotOpenException e) {
			LOG.error("PcapNativeException or NotOpenException :", e);
		}

	}
	
	public void stopPcap4jWrapper() {
      if (handle != null && handle.isOpen()) {
          try {
            handle.breakLoop();
          } catch (NotOpenException noe) {
  			LOG.error("NotOpenException :", noe);
          }
          
          try {
            Thread.sleep(1000);
          } catch (InterruptedException ioe) {
    			LOG.error("InterruptedException :", ioe);
          }
          handle.close();
        }
      	if(dumper!=null) {
      		dumper.close();
      	}

	}
    public String getPCAP_FILE_PATH() {
		return this.PCAP_FILE_PATH;
	}

	public void setPCAP_FILE_PATH(String pCAP_FILE_PATH) {
		this.PCAP_FILE_PATH = pCAP_FILE_PATH;
	}

}
