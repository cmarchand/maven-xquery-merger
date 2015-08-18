/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.inneo.xquery.merger.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ext-cmarchand
 */
public class NamespaceMapping {
    
    private final HashMap<String,String> prefixToNamespace;
    private final HashMap<String,List<String>> namespaceToPrefixes;
    
    public NamespaceMapping() {
        super();
        prefixToNamespace = new HashMap<>();
        namespaceToPrefixes = new HashMap<>();
    }
    
    public void addMapping(final String prefix, final String namespace) throws ParsingException {
        if(namespace==null) throw new ParsingException("ILLEGAL namespace declaration : namespace URI must not be null");
        if("".equals(namespace)) throw new ParsingException("ILLEGAL namespace declaration : namespace URI must not be empty");
        if(prefix==null) throw new ParsingException("ILLEGAL namespace declaration : prefix must not be null");
        if("".equals(prefix)) throw new ParsingException("ILLEGAL namespace declaration : prefix must not be empty");
        String existingNS = getNamespace(prefix);
        if(existingNS!=null && !existingNS.equals(namespace))
            throw new ParsingException("LIMITATION : a prefix must be mapped to a single namespace URI accross all imported modules");
        prefixToNamespace.put(prefix, namespace);
        List<String> prefixes = getPrefixes(namespace);
        if(prefixes==null) {
            prefixes = new ArrayList<>();
            namespaceToPrefixes.put(prefix, prefixes);
        }
        if(!prefixes.contains(prefix)) {
            prefixes.add(prefix);
        }
    }
    
    public String getNamespace(final String prefix) {
        return prefixToNamespace.get(prefix);
    }
    public List<String> getPrefixes(final String namespace) {
        return namespaceToPrefixes.get(namespace);
    }
}
