/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.efl.inneo.xquery.merger;

import fr.efl.inneo.xquery.merger.utils.NamespaceMapping;
import fr.efl.inneo.xquery.merger.utils.ParsingException;
import fr.efl.inneo.xquery.merger.utils.XQueryUriResolver;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

/**
 * Reads a xquery file, and returns the file, with all imported modules included in source code.
 * This class has limitations against XQuery 3.0 syntax, and throws exceptions if xquery file reaches these limitations.
 * 
 * @author ext-cmarchand
 */
public class XQueryMerger {
    private final StreamSource source;
    private final URIResolver uriResolver;
    private Charset charset;
    private NamespaceMapping mapping;
    private static final String CR = System.getProperty("line.separator");
    
    public XQueryMerger(StreamSource source) {
        this(source, Charset.defaultCharset());
    }
    
    public XQueryMerger(StreamSource source, Charset charset) {
        this(source, charset, new XQueryUriResolver());
    }
    public XQueryMerger(StreamSource source, URIResolver uriResolver) {
        this(source, Charset.defaultCharset(), uriResolver);
    }
    
    protected XQueryMerger(StreamSource source, Charset charset, URIResolver uriResolver) {
        super();
        this.source = source;
        this.charset = charset;
        this.uriResolver = uriResolver;
    }
    
    private XQueryMerger setNamespaceMapping(NamespaceMapping mapping) {
        this.mapping = mapping;
        return this;
    }
    
    protected StringBuilder _merge() throws ParsingException {
        if(mapping==null) {
            mapping = new NamespaceMapping();
        }
        // on charge avec l'encoding déterminé
        InputStreamReader isr = new InputStreamReader(source.getInputStream(), charset);
        BufferedReader reader = new BufferedReader(isr);
        StringBuilder result = new StringBuilder();
        try {
            parse(reader, result);
            return result;
        } catch(ParsingException ex) {
            try { reader.close(); } catch(Throwable ignore) {}
            throw ex;
        } catch(IOException ex) {
            throw new ParsingException(ex);
        }
    }
    
    public String merge() throws ParsingException {
        return _merge().toString();
    }
    
    protected StringBuilder parse(BufferedReader reader, StringBuilder result) throws IOException, ParsingException {
        String line = reader.readLine();
        while(line!=null) {
            String clean = ParsingRegex.NORMALIZE_SPACE.matcher(line.trim()).replaceAll(" ");
            if(ParsingRegex.ENCODING_LINE.matcher(clean).matches()) {
                Matcher m = ParsingRegex.QUOTE_EXTRACT.matcher(clean);
                if(m.find()) {
                    String readEncoding = m.group(1);
                    Charset c = Charset.forName(readEncoding);
                    if(!charset.equals(c)) {
                        // on relance le traitement avec le nouvel encoding sur la même source
                        reader.close();
                        System.out.println("reopening "+source.getSystemId()+" with encoding "+c.name());
                        return new XQueryMerger(source, c, uriResolver)._merge();
                    } else {
                        result.append(line).append(CR);
                    }
                } else {
                    result.append(line).append(CR);
                }
            } else if(ParsingRegex.DECLARE_NAMESPACE_LINE.matcher(clean).matches()) {
                Matcher m1 = ParsingRegex.PREFIX_EXTRACT.matcher(clean);
                Matcher m2 = ParsingRegex.NAMESPACE_EXTRACT.matcher(clean);
                if(m1.find() && m2.find()) {
                    String prefix = m1.group(1);
                    String uri = m2.group(1);
                    mapping.addMapping(prefix, uri);
                }
                result.append(line).append(CR);
            } else if(ParsingRegex.IMPORT_MODULE_LINE.matcher(clean).matches()) {
                Matcher m1 = ParsingRegex.PREFIX_EXTRACT.matcher(clean);
                Matcher m2 = ParsingRegex.NAMESPACE_EXTRACT.matcher(clean);
                Matcher m3 = ParsingRegex.MODULE_URL_EXTRACT.matcher(clean);
                if(m1.find() && m2.find() && m3.find()) {
                    String prefix = m1.group(1);
                    String uri = m2.group(1);
                    mapping.addMapping(prefix, uri);
                    String url = m3.group(1);
                    try {
                        StreamSource module = (StreamSource)uriResolver.resolve(url, source.getSystemId());
                        XQueryMerger moduleMerger = new XQueryMerger(module, uriResolver).setNamespaceMapping(mapping);
                        result.append("(: code imported from ").append(url).append(" :)").append(CR);
                        result.append(moduleMerger.merge());
                        result.append("(: end import from ").append(url).append(" :)").append(CR);
                    } catch(TransformerException ex) {
                        throw new ParsingException("Unable to locate "+url+" relative to "+source.getSystemId(), ex);
                    }
                }
            } else {
                result.append(line).append(CR);
            }
        }
        reader.close();
        return result;
    }
    
    public static void main(String[] args) {
        if(args.length!=1) {
            System.err.println("java "+XQueryMerger.class.getName()+" <fileToMerge.xq>");
            System.exit(1);
        } else {
            File f = new File(args[0]);
            if(f.exists() && f.isFile()) {
                StreamSource source = new StreamSource(f);
                XQueryMerger merger = new XQueryMerger(source);
                try {
                    System.out.println(merger.merge());
                } catch(Exception ex) {
                    ex.printStackTrace(System.err);
                    System.exit(2);
                }
            } else {
                System.err.println("Unable to locate "+args[0]+" or it is not a regular file");
                System.exit(1);
            }
        }
    }
    
}
