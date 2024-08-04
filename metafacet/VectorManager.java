/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modnlp.metafacet;

import java.util.Iterator;
import modnlp.tec.client.ConcordanceBrowser;
import modnlp.tec.client.ConcordanceObject;
import modnlp.tec.client.ConcordanceVector;

/**
 *
 * @author shane
 */
public class VectorManager {
ConcordanceBrowser parent;
ConcordanceVector OldVector;
    public VectorManager(ConcordanceBrowser p) 
    {
        parent = p;
        OldVector = (ConcordanceVector) p.getConcordanceVector().clone();
    }
    
    public void updateVector(){
        OldVector = (ConcordanceVector) parent.getConcordanceVector().clone();
    }
    
    public ConcordanceVector getOriginalVector(){
        return OldVector;
    }
    
    public void printstr(String a)
{
    System.out.println(a);
}
    public void removeLine(String filename, String section)
{
    //ConcordanceObject[] toRemove = new ConcordanceObject[];
    ConcordanceVector vec = (ConcordanceVector) parent.getConcordanceVector().clone();
    for (Iterator<ConcordanceObject> iterator = vec.iterator(); iterator.hasNext();) {
        ConcordanceObject next = iterator.next();
        String fn = next.sfilename;
        fn = fn.substring(0, fn.indexOf("."));
        //boolean delete = true; 
        if (fn.trim().equalsIgnoreCase( filename.trim()))
            if (next.sectionID.trim().equalsIgnoreCase(section.trim()))
                       parent.removeConcordanceLineOnly(next);
    }
}
    
public void redisplay() {
    parent.redisplay();
}

public void addLine(String filename, String section){
    //ConcordanceObject[] toRemove = new ConcordanceObject[];
    
    ConcordanceVector vec = OldVector;
    for (Iterator<ConcordanceObject> iterator = vec.iterator(); iterator.hasNext();) {
        ConcordanceVector current = parent.getConcordanceVector();
        ConcordanceObject next = iterator.next();
        String fn = next.sfilename;
        fn = fn.substring(0, fn.indexOf("."));
        //boolean delete = true; 
        if (fn.trim().equalsIgnoreCase( filename.trim()))
            if (next.sectionID.trim().equalsIgnoreCase(section.trim())){
                if (!current.contains(next))
                       parent.addConcordanceLine(next);
            }
    }
   
}
    
     
    
}
