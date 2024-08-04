/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modnlp.metafacet;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import javafx.scene.control.Button;
import javafx.scene.web.WebEngine;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import modnlp.tec.client.ConcordanceBrowser;
import modnlp.tec.client.ConcordanceObject;
import modnlp.tec.client.ConcordanceVector;
import modnlp.tec.client.HeaderProducer;
import modnlp.tec.client.Plugin;
import modnlp.tec.client.TecClientRequest;
import modnlp.tec.client.cache.header.HeaderDownloadThread;
import modnlp.tec.client.cache.header.HeaderCompleteListener;

/**
 *
 * @author shane
 */
public class Metafacet implements Plugin, Runnable, HeaderCompleteListener{
    
    private static ConcordanceBrowser parent = null;

    private static HashMap< String, String> headers = new HashMap<String, String>();
    private static String json ="[{key: \"test\", values:5},{key: \"t1\", values:2}]";
    private static boolean first = true;
    private static ConcordanceVector vec;
    private HeaderDownloadThread headerThread = null;
    private HeaderProducer headerProducer = null;
    private Thread thread;
    private JFrame frame=null;
    private MetafacetContainer meta = null;
    private String dirName = System.getProperty("user.home") + File.separator+"GOKCache" + File.separator+"MetaFacetCache";
    JProgressBar b; 
    private BufferedReader input;
    private String serverStartdate;
    private String cachedDate= "";
    private String pattern = "MM/dd/yyyy HH:mm:ss";
    private DateFormat df = new SimpleDateFormat(pattern);

    @Override
    public void setParent(Object p){
        parent = (ConcordanceBrowser)p;
  }
    
    @Override
    public void activate() {
        
        headerProducer= parent.getHeaderProducer();
        File directory = new File(dirName);
        directory.mkdir();
        serverStartdate = getServerStartDate();
        stop();        
        start();

    } 
    
    
    private void dlHeader() {
    frame = new JFrame("Loading MetaFacet Browser");
    frame.setPreferredSize(new Dimension(400, 60));
    b = new JProgressBar(); 
    // set initial value 
    b.setValue(0); 
    b.setStringPainted(true); 
    frame.add(b);
    frame.pack();
    frame.setVisible(true);
    TecClientRequest request = new TecClientRequest();
    //request.put("request", "nooftokens");
    //look at freq list download and write request in similar fashon
    request.put("request", "dldHeaders");
    if ( parent.subCorpusSelected() )
      request.put("xquerywhere",parent.getXQueryWhere());
    if ( (headerThread != null) ) {
      headerThread.stop();
    }
    if (parent.isStandAlone()) {
        headerThread = new HeaderDownloadThread(headerProducer.getBufferedReader(), request, headers,b);
        headerThread.addListener(this);
        if(meta !=null)
            headerThread.addListener(meta);
        headerThread.addListener(this);
        headerProducer.start();
        headerThread.start();

        //pause while downloading  
    }
    else {

        request.setServerURL("http://"+parent.getRemoteServer());
        request.setServerPORT(parent.getRemotePort()); 
        request.setServerProgramPath("/allheaders");
        headerThread = new HeaderDownloadThread(request, headers,b);
        headerThread.addListener(this);
        if(meta !=null)
           headerThread.addListener(meta);
        headerThread.setEncoding(parent.getEncoding());
        headerThread.start();
        //pause while downloading
     
    }
  }   
    
    public void start() {
    if (thread == null) {
      thread = new Thread(this);
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
  }

  public void stop() {
    if (thread != null) {
      thread = null;
    }
  }

    @Override
    public void run() {  
               //workaround for greek not having a language constant
//        int newLang = parent.getLanguage();
//        if("http://www.genealogiesofknowledge.net/gok/headers-gr/".equals(parent.getHeaderBaseUrl()))
//                    newLang = 5;
        
        String filename = dirName + File.separator + parent.getRemotePort() + parent.getRemoteServer() + "metadata.out";
        //setup
        FileInputStream fis = null;
        ObjectInputStream in = null;
        File test = new File(filename);
        Date cacheDate =null;
        Date serverDate=null;
        
        //checkif cache exists
        String cacheDateStr = getCachedDate();
        if (cacheDateStr.equalsIgnoreCase("")  || serverStartdate.equalsIgnoreCase("") ){
            cacheDate = Calendar.getInstance().getTime();
            serverDate =Calendar.getInstance().getTime();
        }else{
            try{
                cacheDate =df.parse(cacheDateStr);   
                serverDate =df.parse(serverStartdate);
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        
        //if the corpus-language headers have been downloaded
        //and the cache is valid
        if(test.exists() && !cacheDateStr.equalsIgnoreCase("") && serverDate.before(cacheDate)){
            //get from cache
            try {
                fis = new FileInputStream(filename);
                in = new ObjectInputStream(fis);
                headers = null;
                headers = (HashMap< String, String>) in.readObject();
                in.close();
                makeJson();
                if(meta == null){//make metafacet
                    meta = new MetafacetContainer(this,headers,json, vec, parent);
                }
                else{//reshow metafacet and update it
                    meta.setVisible(true);
                    meta.notifyOfThreadComplete(headerThread);
                   
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else{// get headers from server and update cache
            setCachedDate();
            dlHeader();
        }     
    }

    public void makeJson(){
        //cycle through concordance and build metaddata array
        json = "[";
        vec = parent.getConcordanceVector();
        for (Iterator<ConcordanceObject> iterator = vec.iterator(); iterator.hasNext();) {
            ConcordanceObject next = iterator.next();
            String fn = next.sfilename;
            fn= fn.substring(0, fn.indexOf("."));
            String key = fn+ next.sectionID;
            //System.out.println(key);
            if (iterator.hasNext()){
                json += headers.getOrDefault(key, "error")+",";
            }else
                json += headers.getOrDefault(key, "error")+"]";            
        } 
    }
    
    @Override
    public void notifyOfThreadComplete(HeaderDownloadThread thread) {
        frame.dispose();
        // save the object to file
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        
        //workaround for greek not having a language constant
//        int newLang = parent.getLanguage();
//         if("http://www.genealogiesofknowledge.net/gok/headers-gr/".equals(parent.getHeaderBaseUrl()))
//                    newLang = 5;
        try {
            File directory = new File(dirName);
            if(!directory.exists())
                directory.mkdirs();
            String filename = dirName + File.separator + parent.getRemotePort()+ parent.getRemoteServer() + "metadata.out";
            fos = new FileOutputStream(filename);
            out = new ObjectOutputStream(fos);
            out.writeObject(headers);
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        makeJson(); 

        if(meta == null){
            meta = new MetafacetContainer(this,headers,json, vec, parent);
        }else{
             meta.setVisible(true);
              
        }
    }
    public HashMap< String, String> getHeaders(){
        return headers;
    }
    

    private String getServerStartDate() {
    String result = "";
    try{
        if (parent.isStandAlone()) {
            //for now we will just return empty string
        }
        else{
            TecClientRequest clRequest = new TecClientRequest();
            clRequest.setServerURL("http://" + parent.getRemoteServer());
            clRequest.setServerPORT(parent.getRemotePort());
            clRequest.put("request", "serverDate");
            if (parent.isSubCorpusSelectionON()) {
              clRequest.put("xquerywhere", parent.getXQueryWhere());
            }
            clRequest.put("casesensitive", parent.isCaseSensitive() ? "TRUE" : "FALSE");
            clRequest.setServerProgramPath("/freqword");
            URL exturl = new URL(clRequest.toString());
            HttpURLConnection exturlConnection = (HttpURLConnection) exturl.openConnection();
            exturlConnection.setRequestMethod("GET");
            input = new BufferedReader(new InputStreamReader(exturlConnection.getInputStream(), "UTF-8"));
            result = input.readLine();
            exturlConnection.disconnect();
        }
        
      }
     catch (IOException e) {
      System.err.println("Exception: couldn't create stream socket" + e);
    }
    return result;
  }

    private void setCachedDate() {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            String filename = dirName + File.separator+"cdate"+parent.getRemoteServer()+".out";
            fos = new FileOutputStream(filename);
            out = new ObjectOutputStream(fos);
            Date today = Calendar.getInstance().getTime();        
            //Store date as string for caching on client side
            String dateCached = df.format(today);
            out.writeObject(dateCached);
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
     private String getCachedDate() { 
        String result = "";
        FileInputStream fis = null;
        ObjectInputStream in = null;
        String filename = dirName + File.separator+"cdate"+parent.getRemoteServer()+".out";
        File test = new File(filename);
        if(test.exists()){
            try{
               fis = new FileInputStream(filename);
               in = new ObjectInputStream(fis);
               result = (String)in.readObject();
               in.close();
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }
      
}


